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

    @PluginMethod
    public fun getContact(call: PluginCall) {
        try {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
            } else {
                val contactId = call.getString("contactId")

                if (contactId == null) {
                    call.reject("Parameter `contactId` not provided.")
                    return
                }

                val contact = implementation.getContact(contactId, projectionInput(call))

                if (contact == null) {
                    call.reject("Contact not found.")
                    return
                }

                val result = JSObject()
                result.put("contact", contact.jsObject)
                call.resolve(result)
            }
        } catch (exception: Exception) {
            rejectCall(call, exception)
        }
    }

    @PluginMethod
    public fun getContacts(call: PluginCall) {
        try {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
            } else {
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
        } catch (exception: Exception) {
            rejectCall(call, exception)
        }
    }

    @PluginMethod
    public fun createContact(call: PluginCall) {
        try {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
            } else {
                // A call without `contact` has always ended in a NullPointerException that rejects the call below
                val contactId = implementation.createContact(CreateContactInput(call.getObject("contact")!!))

                if (contactId == null) {
                    call.reject("Something went wrong.")
                    return
                }

                val result = JSObject()
                result.put("contactId", contactId)

                call.resolve(result)
            }
        } catch (exception: Exception) {
            rejectCall(call, exception)
        }
    }

    @PluginMethod
    public fun deleteContact(call: PluginCall) {
        try {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
            } else {
                val contactId = call.getString("contactId")

                if (contactId == null) {
                    call.reject("Parameter `contactId` not provided.")
                    return
                }

                if (!implementation.deleteContact(contactId)) {
                    call.reject("Something went wrong.")
                    return
                }

                call.resolve()
            }
        } catch (exception: Exception) {
            rejectCall(call, exception)
        }
    }

    @PluginMethod
    public fun pickContact(call: PluginCall) {
        try {
            if (!isContactsPermissionGranted) {
                requestContactsPermission(call)
            } else {
                val contactPickerIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                startActivityForResult(call, contactPickerIntent, "pickContactResult")
            }
        } catch (exception: Exception) {
            rejectCall(call, exception)
        }
    }

    @ActivityCallback
    private fun pickContactResult(call: PluginCall?, activityResult: ActivityResult) {
        val data = activityResult.data
        if (call != null && activityResult.resultCode == Activity.RESULT_OK && data != null) {
            // This will return a URI for retrieving the contact, e.g.: "content://com.android.contacts/contacts/1234"
            val uri = data.data
            // Parse the contactId from this URI.
            val contactId = Contacts.getIdFromUri(uri)

            if (contactId == null) {
                call.reject("Parameter `contactId` not returned from pick. Please raise an issue in GitHub if this problem persists.")
                return
            }

            val contact = implementation.getContact(contactId, projectionInput(call))

            if (contact == null) {
                call.reject("Contact not found.")
                return
            }

            val result = JSObject()
            result.put("contact", contact.jsObject)
            call.resolve(result)
        }
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
    }
}
