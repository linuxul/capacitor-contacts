package getcapacitor.community.contacts

import android.database.Cursor
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.util.Base64
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.net.URLConnection

public class ContactPayload internal constructor(
    // Id
    private val contactId: String?
) {
    // Name
    private val name = JSObject()

    // Organization
    private val organization = JSObject()

    // Birthday
    private val birthday = JSObject()

    // Note
    private var note: String? = null

    // Phones
    private val phones = JSArray()

    // Emails
    private val emails = JSArray()

    // URLs
    private val urls = JSArray()

    // Postal Addresses
    private val postalAddresses = JSArray()

    // Image
    private val image = JSObject()

    private fun parseStrToIntSafe(str: String): Int? = try {
        Integer.parseInt(str)
    } catch (e: NumberFormatException) {
        null
    }

    private fun getBooleanByColumnName(cursor: Cursor, columnName: String): Boolean? {
        val index = cursor.getColumnIndex(columnName)
        if (index >= 0) {
            return cursor.getInt(index) == 1
        }
        return null
    }

    private fun getStringByColumnName(cursor: Cursor, columnName: String): String? {
        val index = cursor.getColumnIndex(columnName)
        if (index >= 0) {
            return cursor.getString(index)
        }
        return null
    }

    private fun getIntByColumnName(cursor: Cursor, columnName: String): Int? {
        val index = cursor.getColumnIndex(columnName)
        if (index >= 0) {
            return cursor.getInt(index)
        }
        return null
    }

    private fun getBase64ByColumnName(cursor: Cursor, columnName: String): String? {
        val index = cursor.getColumnIndex(columnName)
        if (index >= 0) {
            val blob = cursor.getBlob(index)
            if (blob != null) {
                val mimeType = getMimetype(blob)
                val encodedImage = Base64.encodeToString(blob, Base64.NO_WRAP)
                return "data:$mimeType;base64,$encodedImage"
            }
        }
        return null
    }

    public fun fillDataByCursor(cursor: Cursor) {
        val mimeType = getStringByColumnName(cursor, ContactsContract.Data.MIMETYPE) ?: return

        when (mimeType) {
            // Name
            StructuredName.CONTENT_ITEM_TYPE -> {
                name.put("display", getStringByColumnName(cursor, StructuredName.DISPLAY_NAME))
                name.put("given", getStringByColumnName(cursor, StructuredName.GIVEN_NAME))
                name.put("middle", getStringByColumnName(cursor, StructuredName.MIDDLE_NAME))
                name.put("family", getStringByColumnName(cursor, StructuredName.FAMILY_NAME))
                name.put("prefix", getStringByColumnName(cursor, StructuredName.PREFIX))
                name.put("suffix", getStringByColumnName(cursor, StructuredName.SUFFIX))
            }

            // Organization
            Organization.CONTENT_ITEM_TYPE -> {
                organization.put("company", getStringByColumnName(cursor, Organization.COMPANY))
                organization.put("jobTitle", getStringByColumnName(cursor, Organization.TITLE))
                organization.put("department", getStringByColumnName(cursor, Organization.DEPARTMENT))
            }

            // Birthday
            Event.CONTENT_ITEM_TYPE -> {
                val eventType = getIntByColumnName(cursor, Event.TYPE)
                if (eventType == Event.TYPE_BIRTHDAY) {
                    val birthdayString = getStringByColumnName(cursor, Event.START_DATE)

                    if (birthdayString != null) {
                        // Normally the date is formatted as "yyyy-mm-dd"
                        // But it's possible to not have a year saved to the birthday,
                        // then it's formatted like "--mm-dd".
                        // So we'll have to rectify that.
                        // Next we'll split the string into pieces; like String.split in Java, without the trailing empty ones:
                        val splittedBirthdayString = birthdayString.replace("--", "").split("-").dropLastWhile { it.isEmpty() }

                        if (splittedBirthdayString.size == 2) {
                            // Birthday is formatted like "mm-dd"
                            birthday.put("month", parseStrToIntSafe(splittedBirthdayString[0]))
                            birthday.put("day", parseStrToIntSafe(splittedBirthdayString[1]))
                        } else if (splittedBirthdayString.size == 3) {
                            // Birthday is formatted like "yyyy-mm-dd"
                            birthday.put("year", parseStrToIntSafe(splittedBirthdayString[0]))
                            birthday.put("month", parseStrToIntSafe(splittedBirthdayString[1]))
                            birthday.put("day", parseStrToIntSafe(splittedBirthdayString[2]))
                        }
                    }
                }
            }

            // Note
            Note.CONTENT_ITEM_TYPE -> note = getStringByColumnName(cursor, Note.NOTE)

            // Phones
            Phone.CONTENT_ITEM_TYPE -> {
                val number = getStringByColumnName(cursor, Phone.NUMBER)
                val phoneType = getIntByColumnName(cursor, Phone.TYPE)
                if (number != null && phoneType != null) {
                    val phoneObject = JSObject()
                    phoneObject.put("type", Contacts.phoneTypeMap.getKey(phoneType))
                    phoneObject.put("label", getStringByColumnName(cursor, Phone.LABEL))
                    phoneObject.put("isPrimary", getBooleanByColumnName(cursor, Phone.IS_PRIMARY))
                    phoneObject.put("number", number)
                    phones.put(phoneObject)
                }
            }

            // Emails
            Email.CONTENT_ITEM_TYPE -> {
                val address = getStringByColumnName(cursor, Email.ADDRESS)
                val emailType = getIntByColumnName(cursor, Email.TYPE)
                if (address != null && emailType != null) {
                    val emailObject = JSObject()
                    emailObject.put("type", Contacts.emailTypeMap.getKey(emailType))
                    emailObject.put("label", getStringByColumnName(cursor, Email.LABEL))
                    emailObject.put("isPrimary", getBooleanByColumnName(cursor, Email.IS_PRIMARY))
                    emailObject.put("address", address)
                    emails.put(emailObject)
                }
            }

            // URLs
            Website.CONTENT_ITEM_TYPE -> urls.put(getStringByColumnName(cursor, Website.URL))

            // Postal Addresses
            StructuredPostal.CONTENT_ITEM_TYPE -> {
                val postalAddressType = getIntByColumnName(cursor, StructuredPostal.TYPE)
                if (postalAddressType != null) {
                    val postalAddressObject = JSObject()
                    postalAddressObject.put("type", Contacts.postalAddressTypeMap.getKey(postalAddressType))
                    postalAddressObject.put("label", getStringByColumnName(cursor, StructuredPostal.LABEL))
                    postalAddressObject.put("isPrimary", getBooleanByColumnName(cursor, StructuredPostal.IS_PRIMARY))
                    postalAddressObject.put("formatted", getStringByColumnName(cursor, StructuredPostal.FORMATTED_ADDRESS))
                    postalAddressObject.put("street", getStringByColumnName(cursor, StructuredPostal.STREET))
                    postalAddressObject.put("neighborhood", getStringByColumnName(cursor, StructuredPostal.NEIGHBORHOOD))
                    postalAddressObject.put("city", getStringByColumnName(cursor, StructuredPostal.CITY))
                    postalAddressObject.put("region", getStringByColumnName(cursor, StructuredPostal.REGION))
                    postalAddressObject.put("postcode", getStringByColumnName(cursor, StructuredPostal.POSTCODE))
                    postalAddressObject.put("country", getStringByColumnName(cursor, StructuredPostal.COUNTRY))
                    postalAddresses.put(postalAddressObject)
                }
            }

            // Image
            Photo.CONTENT_ITEM_TYPE -> {
                val base64String = getBase64ByColumnName(cursor, Photo.PHOTO)
                if (base64String != null) {
                    image.put("base64String", base64String)
                }
            }
        }
    }

    @get:JvmName("getJSObject")
    public val jsObject: JSObject
        get() {
            val contact = JSObject()

            // Id
            contact.put("contactId", contactId)

            // Name
            if (name.length() > 0) {
                contact.put("name", name)
            }

            // Organization
            if (organization.length() > 0) {
                contact.put("organization", organization)
            }

            // Birthday
            if (birthday.length() > 0) {
                contact.put("birthday", birthday)
            }

            // Note
            if (note != null) {
                contact.put("note", note)
            }

            // Phones
            if (phones.length() > 0) {
                contact.put("phones", phones)
            }

            // Emails
            if (emails.length() > 0) {
                contact.put("emails", emails)
            }

            // URLs
            if (urls.length() > 0) {
                contact.put("urls", urls)
            }

            // Postal Addresses
            if (postalAddresses.length() > 0) {
                contact.put("postalAddresses", postalAddresses)
            }

            // Image
            if (image.length() > 0) {
                contact.put("image", image)
            }

            return contact
        }

    private companion object {
        private fun getMimetype(blob: ByteArray): String = try {
            val stream = BufferedInputStream(ByteArrayInputStream(blob))
            URLConnection.guessContentTypeFromStream(stream) ?: "image/png"
        } catch (e: Exception) {
            "image/png"
        }
    }
}
