package getcapacitor.community.contacts

import android.provider.ContactsContract
import org.json.JSONObject

public class GetContactsProjectionInput internal constructor(fromJSONObject: JSONObject) {
    // Name
    private val name: Boolean = fromJSONObject.optBoolean("name")

    // Organization
    private val organization: Boolean = fromJSONObject.optBoolean("organization")

    // Birthday
    private val birthday: Boolean = fromJSONObject.optBoolean("birthday")

    // Note
    private val note: Boolean = fromJSONObject.optBoolean("note")

    // Phones
    private val phones: Boolean = fromJSONObject.optBoolean("phones")

    // Emails
    private val emails: Boolean = fromJSONObject.optBoolean("emails")

    // URLs
    private val urls: Boolean = fromJSONObject.optBoolean("urls")

    // Postal Addresses
    private val postalAddresses: Boolean = fromJSONObject.optBoolean("postalAddresses")

    // Image
    private val image: Boolean = fromJSONObject.optBoolean("image")

    public val projection: Array<String>
        get() {
            val projection = ArrayList<String>()

            projection.add(ContactsContract.Data.MIMETYPE)
            projection.add(ContactsContract.Data._ID)
            projection.add(ContactsContract.Data.CONTACT_ID)

            // Name
            if (name) {
                projection.add(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
                projection.add(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
                projection.add(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)
                projection.add(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
                projection.add(ContactsContract.CommonDataKinds.StructuredName.PREFIX)
                projection.add(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)
            }

            // Organization
            if (organization) {
                projection.add(ContactsContract.CommonDataKinds.Organization.COMPANY)
                projection.add(ContactsContract.CommonDataKinds.Organization.TITLE)
                projection.add(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)
            }

            // Birthday
            if (birthday) {
                projection.add(ContactsContract.CommonDataKinds.Event.START_DATE)
                projection.add(ContactsContract.CommonDataKinds.Event.TYPE)
            }

            // Note
            if (note) {
                projection.add(ContactsContract.CommonDataKinds.Note.NOTE)
            }

            // Phones
            if (phones) {
                projection.add(ContactsContract.CommonDataKinds.Phone.TYPE)
                projection.add(ContactsContract.CommonDataKinds.Phone.LABEL)
                projection.add(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                projection.add(ContactsContract.CommonDataKinds.Phone.NUMBER)
                // projection.add(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            }

            // Emails
            if (emails) {
                projection.add(ContactsContract.CommonDataKinds.Email.TYPE)
                projection.add(ContactsContract.CommonDataKinds.Email.LABEL)
                projection.add(ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
                projection.add(ContactsContract.CommonDataKinds.Email.ADDRESS)
            }

            // URLs
            if (urls) {
                projection.add(ContactsContract.CommonDataKinds.Website.URL)
            }

            // Postal Addresses
            if (postalAddresses) {
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.IS_PRIMARY)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
                projection.add(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
            }

            // Image
            if (image) {
                projection.add(ContactsContract.Contacts.Photo.PHOTO)
            }

            return projection.toTypedArray()
        }

    public val selectionArgs: Array<String>
        get() {
            val selectionArgs = ArrayList<String>()

            // Name
            if (name) {
                selectionArgs.add(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            }

            // Organization
            if (organization) {
                selectionArgs.add(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
            }

            // Birthday
            if (birthday) {
                selectionArgs.add(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
            }

            // Note
            if (note) {
                selectionArgs.add(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
            }

            // Phones
            if (phones) {
                selectionArgs.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            }

            // Emails
            if (emails) {
                selectionArgs.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
            }

            // URLs
            if (urls) {
                selectionArgs.add(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
            }

            // Postal Addresses
            if (postalAddresses) {
                selectionArgs.add(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
            }

            // Image
            if (image) {
                selectionArgs.add(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            }

            return selectionArgs.toTypedArray()
        }

    public companion object {
        @JvmStatic
        public fun getSelection(selectionArgs: Array<String>): String {
            if (selectionArgs.isEmpty()) {
                return ""
            }
            return ContactsContract.Data.MIMETYPE + " in (" + selectionArgs.joinToString(", ") { "?" } + ")"
        }
    }
}
