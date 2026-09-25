package getcapacitor.community.contacts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Logger
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginException
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import java.util.concurrent.Executors

@CapacitorPlugin(
    name = "Contacts",
    permissions = [Permission(strings = [Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS], alias = "contacts")]
)
public class ContactsPlugin : Plugin() {
    private lateinit var implementation: Contacts

    override fun load() {
        implementation = Contacts(activity)
    }

    private fun requestContactsPermission(call: PluginCall) {
        requestPermissionForAlias("contacts", call, "permissionCallback")
    }

    /**
     * Checks the the given permission is granted or not
     * @return Returns true if the permission is granted and false if it is denied.
     */
    private val isContactsPermissionGranted: Boolean
        get() = getPermissionState("contacts") == PermissionState.GRANTED

    @PermissionCallback
    private fun permissionCallback(call: PluginCall) {
        if (!isContactsPermissionGranted) {
            call.reject("Permission is required to access contacts.")
            return
        }

        when (call.methodName) {
            "getContact" -> getContact(call)
            "getContacts" -> getContacts(call)
            "createContact" -> createContact(call)
            "deleteContact" -> deleteContact(call)
            "pickContact" -> pickContact(call)
        }
    }

    /**
     * Runs [block], and rejects [call] with the message of any exception it throws, as the Java implementation did.
     * A [PluginException] is rethrown, so that the bridge rejects the call with its message and code.
     */
    private inline fun rejectingFailures(call: PluginCall, block: () -> Unit) {
        try {
            block()
        } catch (exception: PluginException) {
            throw exception
        } catch (exception: Exception) {
            rejectCall(call, exception)
        }
    }

    @PluginMethod
    public fun getContact(call: PluginCall) {
        rejectingFailures(call) {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
                return
            }

            val contactId = call.getString("contactId") ?: throw PluginException("Parameter `contactId` not provided.")
            val contact = implementation.getContact(contactId, projectionInput(call)) ?: throw PluginException("Contact not found.")

            val result = JSObject()
            result.put("contact", contact.jsObject)
            call.resolve(result)
        }
    }

    @PluginMethod
    public fun getContacts(call: PluginCall) {
        rejectingFailures(call) {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
                return
            }

            val executor = Executors.newSingleThreadExecutor()

            executor.execute {
                try {
                    val contacts = implementation.getContacts(projectionInput(call))

                    val contactsJSArray = JSArray()
                    for (contact in contacts.values) {
                        contactsJSArray.put(contact.jsObject)
                    }

                    val result = JSObject()
                    result.put("contacts", contactsJSArray)

                    bridge.activity.runOnUiThread { call.resolve(result) }
                } catch (exception: Exception) {
                    rejectCall(call, exception)
                }
            }

            executor.shutdown()
        }
    }

    @PluginMethod
    public fun createContact(call: PluginCall) {
        rejectingFailures(call) {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
                return
            }

            // A call without `contact` has always ended in a NullPointerException that rejects the call
            val contactId =
                implementation.createContact(CreateContactInput(call.getObject("contact")!!))
                    ?: throw PluginException("Something went wrong.")

            val result = JSObject()
            result.put("contactId", contactId)

            call.resolve(result)
        }
    }

    @PluginMethod
    public fun deleteContact(call: PluginCall) {
        rejectingFailures(call) {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
                return
            }

            val contactId = call.getString("contactId") ?: throw PluginException("Parameter `contactId` not provided.")

            if (!implementation.deleteContact(contactId)) {
                throw PluginException("Something went wrong.")
            }

            call.resolve()
        }
    }

    @PluginMethod
    public fun pickContact(call: PluginCall) {
        rejectingFailures(call) {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
                return
            }

            val contactPickerIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(call, contactPickerIntent, "pickContactResult")
        }
    }

    @ActivityCallback
    private fun pickContactResult(call: PluginCall?, activityResult: ActivityResult) {
        if (call == null) {
            return
        }
        if (activityResult.resultCode != Activity.RESULT_OK) {
            throw PluginException(PICKER_CANCELLED_MESSAGE)
        }

        // This will return a URI for retrieving the contact, e.g.: "content://com.android.contacts/contacts/1234"
        val uri = activityResult.data?.data
        // Parse the contactId from this URI.
        val contactId = Contacts.getIdFromUri(uri) ?: throw PluginException(CONTACT_ID_NOT_RETURNED_MESSAGE)

        val contact = implementation.getContact(contactId, projectionInput(call)) ?: throw PluginException("Contact not found.")

        val result = JSObject()
        result.put("contact", contact.jsObject)
        call.resolve(result)
    }

    // A call without `projection` has always ended in a NullPointerException
    private fun projectionInput(call: PluginCall): GetContactsProjectionInput = GetContactsProjectionInput(call.getObject("projection")!!)

    private fun rejectCall(call: PluginCall, exception: Exception) {
        val message = exception.message ?: "An error occurred."
        Logger.error(TAG, message, exception)
        call.reject(message)
    }

    public companion object {
        public const val TAG: String = "Contacts"

        private const val PICKER_CANCELLED_MESSAGE = "User cancelled the contact picker."
        private const val CONTACT_ID_NOT_RETURNED_MESSAGE =
            "Parameter `contactId` not returned from pick. Please raise an issue in GitHub if this problem persists."
    }
}
