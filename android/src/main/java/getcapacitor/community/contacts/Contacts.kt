package getcapacitor.community.contacts

import android.app.Activity
import android.content.ContentProviderOperation
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.RawContacts
import android.util.Base64

public class Contacts internal constructor(private val activity: Activity) {
    public fun getContact(contactId: String, projectionInput: GetContactsProjectionInput): ContactPayload? {
        val projection = projectionInput.projection

        val selection = ContactsContract.RawContacts.CONTACT_ID + " = ?"
        val selectionArgs = arrayOf(contactId)

        val cursor = activity.contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null)

        return cursor?.use {
            if (it.count > 0) {
                val contact = ContactPayload(contactId)

                while (it.moveToNext()) {
                    contact.fillDataByCursor(it)
                }

                contact
            } else {
                null
            }
        }
    }

    public fun getContacts(projectionInput: GetContactsProjectionInput): HashMap<String?, ContactPayload> {
        val projection = projectionInput.projection

        // val selectionArgs = projectionInput.selectionArgs

        // val selection = GetContactsProjectionInput.getSelection(selectionArgs)

        val contacts = HashMap<String?, ContactPayload>()

        // val cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, if (selectionArgs.isNotEmpty()) selection else null, if (selectionArgs.isNotEmpty()) selectionArgs else null, null)
        val cursor = activity.contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, null, null, null)

        cursor?.use {
            if (it.count > 0) {
                while (it.moveToNext()) {
                    // val _id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                    val contactId: String? = it.getString(it.getColumnIndex(ContactsContract.Data.CONTACT_ID))

                    contacts.getOrPut(contactId) { ContactPayload(contactId) }.fillDataByCursor(it)
                }
            }
        }

        return contacts
    }

    public fun createContact(contactInput: CreateContactInput): String? {
        val ops = ArrayList<ContentProviderOperation>()
        var op =
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
        ops.add(op.build())

        // Name
        op =
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                // Add to this key
                .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                // Data
                // .withValue(StructuredName.DISPLAY_NAME, name)
                .withValue(StructuredName.GIVEN_NAME, contactInput.nameGiven)
                .withValue(StructuredName.MIDDLE_NAME, contactInput.nameMiddle)
                .withValue(StructuredName.FAMILY_NAME, contactInput.nameFamily)
                .withValue(StructuredName.PREFIX, contactInput.namePrefix)
                .withValue(StructuredName.SUFFIX, contactInput.nameSuffix)
        ops.add(op.build())

        // Organization
        op =
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                // Add to this key
                .withValue(ContactsContract.Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                // Data
                .withValue(Organization.COMPANY, contactInput.organizationName)
                .withValue(Organization.TITLE, contactInput.organizationJobTitle)
                .withValue(Organization.DEPARTMENT, contactInput.organizationDepartment)
        ops.add(op.build())

        // Birthday
        op =
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                // Add to this key
                .withValue(ContactsContract.Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                // Data
                .withValue(Event.START_DATE, contactInput.birthday)
                .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
        ops.add(op.build())

        // Note
        op =
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                // Add to this key
                .withValue(ContactsContract.Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                // Data
                .withValue(Note.NOTE, contactInput.note)
        ops.add(op.build())

        // @TODO not sure where to allow yields
        // https://www.grokkingandroid.com/androids-contentprovideroperation-withyieldallowed-explained/
        op.withYieldAllowed(true)

        // Phones
        for (phone in contactInput.phones) {
            op =
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    // Add to this key
                    .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    // Data
                    .withValue(Phone.TYPE, phone.type)
                    .withValue(Phone.LABEL, phone.label)
                    .withValue(Phone.NUMBER, phone.number)
            if (phone.isPrimary) {
                op.withValue(Phone.IS_PRIMARY, true)
            }
            ops.add(op.build())
        }

        // Emails
        for (email in contactInput.emails) {
            op =
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    // Add to this key
                    .withValue(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    // Data
                    .withValue(Email.TYPE, email.type)
                    .withValue(Email.LABEL, email.label)
                    .withValue(Email.ADDRESS, email.address)
            if (email.isPrimary) {
                op.withValue(Email.IS_PRIMARY, true)
            }
            ops.add(op.build())
        }

        // URLs
        for (url in contactInput.urls) {
            op =
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    // Add to this key
                    .withValue(ContactsContract.Data.MIMETYPE, Website.CONTENT_ITEM_TYPE)
                    // Data
                    .withValue(Website.URL, url)
            ops.add(op.build())
        }

        // Postal Addresses
        for (address in contactInput.postalAddresses) {
            op =
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    // Add to this key
                    .withValue(ContactsContract.Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                    // Data
                    .withValue(StructuredPostal.TYPE, address.type)
                    .withValue(StructuredPostal.LABEL, address.label)
                    .withValue(StructuredPostal.STREET, address.street)
                    .withValue(StructuredPostal.NEIGHBORHOOD, address.neighborhood)
                    .withValue(StructuredPostal.CITY, address.city)
                    .withValue(StructuredPostal.REGION, address.region)
                    .withValue(StructuredPostal.POSTCODE, address.postcode)
                    .withValue(StructuredPostal.COUNTRY, address.country)
            if (address.isPrimary) {
                op.withValue(StructuredPostal.IS_PRIMARY, true)
            }
            ops.add(op.build())
        }

        // Image
        val base64String = contactInput.image?.base64String
        if (base64String != null) {
            val photoData = Base64.decode(base64String, Base64.DEFAULT)

            op =
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    // Add to this key
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    // Data
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoData)
            ops.add(op.build())
        }

        try {
            val result = activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

            // This will return a URI for retrieving the contact, e.g.: "content://com.android.contacts/raw_contacts/1234"
            val uri = result.firstOrNull()?.uri
            // Parse the rawId from this URI.
            val rawId = getIdFromUri(uri)
            if (rawId != null) {
                // Get the contactId from the rawId and return it.
                return getContactIdByRawId(rawId)
            }
        } catch (e: Exception) {
            // @TODO: we might want to throw an error somehow
        }

        return null
    }

    public fun deleteContact(contactId: String): Boolean {
        try {
            // This will compose a URI, used for deleting that specific contact,
            // e.g.: "content://com.android.contacts/contacts/1234"
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            val deleted = activity.contentResolver.delete(uri, null, null)
            return deleted > 0
        } catch (e: Exception) {
            // Something went wrong
            return false
        }
    }

    private fun getContactIdByRawId(contactRawId: String): String? {
        val projection = arrayOf(ContactsContract.RawContacts.CONTACT_ID)

        val selection = ContactsContract.RawContacts._ID + "= ?"
        val selectionArgs = arrayOf(contactRawId)

        val cursor = activity.contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null)

        return cursor?.use {
            if (it.moveToNext()) {
                val index = it.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                if (index >= 0) it.getString(index) else null
            } else {
                null
            }
        }
    }

    public companion object {
        @JvmField
        public val phoneTypeMap: BiMap<String, Int> =
            BiMap(
                hashMapOf(
                    // Home / Work
                    "home" to Phone.TYPE_HOME,
                    "work" to Phone.TYPE_WORK,
                    // Other
                    "other" to Phone.TYPE_OTHER,
                    // Custom
                    "custom" to Phone.TYPE_CUSTOM,
                    // Phone specific
                    "main" to Phone.TYPE_MAIN,
                    "mobile" to Phone.TYPE_MOBILE,
                    "pager" to Phone.TYPE_PAGER,
                    "fax_work" to Phone.TYPE_FAX_WORK,
                    "fax_home" to Phone.TYPE_FAX_HOME,
                    "fax_other" to Phone.TYPE_OTHER_FAX,
                    //
                    //
                    // Android only:
                    // Phone specific
                    "callback" to Phone.TYPE_CALLBACK,
                    "car" to Phone.TYPE_CAR,
                    "company_main" to Phone.TYPE_COMPANY_MAIN,
                    "isdn" to Phone.TYPE_ISDN,
                    "radio" to Phone.TYPE_RADIO,
                    "telex" to Phone.TYPE_TELEX,
                    "tty_tdd" to Phone.TYPE_TTY_TDD,
                    "work_mobile" to Phone.TYPE_WORK_MOBILE,
                    "work_pager" to Phone.TYPE_WORK_PAGER,
                    "assistant" to Phone.TYPE_ASSISTANT,
                    "mms" to Phone.TYPE_MMS
                ),
                "custom",
                Phone.TYPE_CUSTOM
            )

        @JvmField
        public val emailTypeMap: BiMap<String, Int> =
            BiMap(
                hashMapOf(
                    // Home / Work
                    "home" to Email.TYPE_HOME,
                    "work" to Email.TYPE_WORK,
                    // Other
                    "other" to Email.TYPE_OTHER,
                    // Custom
                    "custom" to Email.TYPE_CUSTOM,
                    //
                    //
                    // Android only:
                    // Email specific
                    "mobile" to Email.TYPE_MOBILE
                ),
                "custom",
                Email.TYPE_CUSTOM
            )

        @JvmField
        public val postalAddressTypeMap: BiMap<String, Int> =
            BiMap(
                hashMapOf(
                    // Home / Work
                    "home" to StructuredPostal.TYPE_HOME,
                    "work" to StructuredPostal.TYPE_WORK,
                    // Other
                    "other" to StructuredPostal.TYPE_OTHER,
                    // Custom
                    "custom" to StructuredPostal.TYPE_CUSTOM
                ),
                "custom",
                StructuredPostal.TYPE_CUSTOM
            )

        @JvmStatic
        public fun getIdFromUri(uri: Uri?): String? = uri?.lastPathSegment
        // If we want to support long, we can do something like this instead:
        // val id = ContentUris.parseId(uri)
        // if (id == -1L) {
        //    return null
        // }
        // return id
    }
}
