package getcapacitor.community.contacts

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import org.json.JSONObject

public class CreateContactInput internal constructor(fromJSONObject: JSONObject) {
    // @TODO:
    // Contact Type

    // Name
    public var nameGiven: String? = null
    public var nameMiddle: String? = null
    public var nameFamily: String? = null
    public var namePrefix: String? = null
    public var nameSuffix: String? = null
    // private val nickname: String?

    // Organization
    public var organizationName: String? = null
    public var organizationJobTitle: String? = null
    public var organizationDepartment: String? = null

    // Birthday
    public var birthday: String? = null

    // Note
    public var note: String? = null

    // Phones
    public var phones: ArrayList<PhoneInput> = ArrayList()

    // Emails
    public var emails: ArrayList<EmailInput> = ArrayList()

    // URLs
    public var urls: ArrayList<String> = ArrayList()

    // Postal Addresses
    public var postalAddresses: ArrayList<PostalAddressInput> = ArrayList()

    // Image
    public var image: ImageInput? = null

    init {
        // Name
        val nameObject = fromJSONObject.optJSONObject("name")
        if (nameObject != null) {
            nameGiven = nameObject.optStringOrNull("given")
            nameMiddle = nameObject.optStringOrNull("middle")
            nameFamily = nameObject.optStringOrNull("family")
            namePrefix = nameObject.optStringOrNull("prefix")
            nameSuffix = nameObject.optStringOrNull("suffix")
        }

        // Organization
        val organizationObject = fromJSONObject.optJSONObject("organization")
        if (organizationObject != null) {
            organizationName = organizationObject.optStringOrNull("company")
            organizationJobTitle = organizationObject.optStringOrNull("jobTitle")
            organizationDepartment = organizationObject.optStringOrNull("department")
        }

        // Birthday
        val birthdayObject = fromJSONObject.optJSONObject("birthday")
        if (birthdayObject != null) {
            if (birthdayObject.has("month") && birthdayObject.has("day")) {
                val month = birthdayObject.optString("month")
                val day = birthdayObject.optString("day")

                var birthdayFormat = "MM-dd"
                var birthdayString = "$month-$day"

                if (birthdayObject.has("year")) {
                    val year = birthdayObject.optString("year")

                    birthdayFormat = "yyyy-MM-dd"
                    birthdayString = "$year-$birthdayString"
                }

                try {
                    val dateFormat = SimpleDateFormat(birthdayFormat, Locale.getDefault())
                    // `isLenient` makes sure input like "2020-06-50" does not get parsed to a valid date.
                    dateFormat.isLenient = false
                    val date = dateFormat.parse(birthdayString)
                    if (date != null) {
                        birthday =
                            if (birthdayFormat == "MM-dd") {
                                // We have to prefix with "--",
                                // because otherwise it may get saved with the current year attached instead of no year at all.
                                "--$birthdayString"
                            } else {
                                birthdayString
                            }
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                    // Something wrong with this date format
                }
            }
        }

        // Note
        note = fromJSONObject.optStringOrNull("note")

        // Phones
        val phonesArray = fromJSONObject.optJSONArray("phones")
        if (phonesArray != null) {
            for (n in 0 until phonesArray.length()) {
                phonesArray.optJSONObject(n)?.let { phones.add(PhoneInput(it)) }
            }
        }

        // Emails
        val emailsArray = fromJSONObject.optJSONArray("emails")
        if (emailsArray != null) {
            for (n in 0 until emailsArray.length()) {
                emailsArray.optJSONObject(n)?.let { emails.add(EmailInput(it)) }
            }
        }

        // URLs
        val urlsArray = fromJSONObject.optJSONArray("urls")
        if (urlsArray != null) {
            for (n in 0 until urlsArray.length()) {
                urls.add(urlsArray.optString(n))
            }
        }

        // Postal Addresses
        val postalAddressesArray = fromJSONObject.optJSONArray("postalAddresses")
        if (postalAddressesArray != null) {
            for (n in 0 until postalAddressesArray.length()) {
                postalAddressesArray.optJSONObject(n)?.let { postalAddresses.add(PostalAddressInput(it)) }
            }
        }

        // Image
        val imageObject = fromJSONObject.optJSONObject("image")
        if (imageObject != null) {
            image = ImageInput(imageObject)
        }
    }

    public class PhoneInput internal constructor(fromJSONObject: JSONObject) {
        public val type: Int = Contacts.phoneTypeMap.getValue(fromJSONObject.optString("type"))
        public val label: String? = fromJSONObject.optStringOrNull("label")
        public val isPrimary: Boolean = fromJSONObject.optBoolean("isPrimary", false)

        public val number: String? = fromJSONObject.optStringOrNull("number")
    }

    public class EmailInput internal constructor(fromJSONObject: JSONObject) {
        public val type: Int = Contacts.emailTypeMap.getValue(fromJSONObject.optString("type"))
        public val label: String? = fromJSONObject.optStringOrNull("label")
        public val isPrimary: Boolean = fromJSONObject.optBoolean("isPrimary", false)

        public val address: String? = fromJSONObject.optStringOrNull("address")
    }

    public class PostalAddressInput internal constructor(fromJSONObject: JSONObject) {
        public val type: Int = Contacts.postalAddressTypeMap.getValue(fromJSONObject.optString("type"))
        public val label: String? = fromJSONObject.optStringOrNull("label")
        public val isPrimary: Boolean = fromJSONObject.optBoolean("isPrimary", false)

        public val street: String? = fromJSONObject.optStringOrNull("street")
        public val neighborhood: String? = fromJSONObject.optStringOrNull("neighborhood")
        public val city: String? = fromJSONObject.optStringOrNull("city")
        public val region: String? = fromJSONObject.optStringOrNull("region")
        public val postcode: String? = fromJSONObject.optStringOrNull("postcode")
        public val country: String? = fromJSONObject.optStringOrNull("country")
    }

    public class ImageInput internal constructor(fromJSONObject: JSONObject) {
        public val base64String: String? = fromJSONObject.optStringOrNull("base64String")
    }

    private companion object {
        /** The value as a string when the key is present, even if it is JSON null. */
        private fun JSONObject.optStringOrNull(key: String): String? = if (has(key)) optString(key) else null
    }
}
