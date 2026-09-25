import Foundation
import UIKit
import Capacitor
import Contacts
import ContactsUI

@objc(ContactsPlugin)
public class ContactsPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "ContactsPlugin"
    public let jsName = "Contacts"
    public let pluginMethods: [CAPPluginMethod] = [
        .promise("checkPermissions", ContactsPlugin.checkPermissions),
        .async("requestPermissions", ContactsPlugin.requestContactsPermissions),
        .promise("getContact", ContactsPlugin.getContact),
        .promise("getContacts", ContactsPlugin.getContacts),
        .promise("createContact", ContactsPlugin.createContact),
        .promise("deleteContact", ContactsPlugin.deleteContact),
        .async("pickContact", ContactsPlugin.pickContact)
    ]
    static let permissionRequiredMessage = "Permission is required to access contacts."
    static let noPresenterMessage = "Unable to display the contact picker: there is no view controller to present it from"
    static let pickerCancelledMessage = "User cancelled the contact picker."
    private static var pickerDelegateKey: UInt8 = 0

    private let implementation = Contacts()

    override public func checkPermissions(_ call: CAPPluginCall) {
        call.resolve(Self.permissionResult(CNContactStore.authorizationStatus(for: .contacts)))
    }

    /// Asks for contacts access and returns the resulting state, as `checkPermissions` does. Registered as
    /// `requestPermissions`: an async method cannot override the synchronous `CAPPlugin.requestPermissions`.
    func requestContactsPermissions(_ call: CAPPluginCall) async throws -> JSObject {
        // As before, a failed request is not an error: the call answers the state the request left.
        _ = try? await CNContactStore().requestAccess(for: .contacts)
        return Self.permissionResult(CNContactStore.authorizationStatus(for: .contacts))
    }

    /// `{ contacts }` with the permission state for `status`.
    static func permissionResult(_ status: CNAuthorizationStatus) -> JSObject {
        let permissionState: String

        switch status {
        case .notDetermined:
            permissionState = "prompt"
        case .restricted, .denied:
            permissionState = "denied"
        case .authorized:
            permissionState = "granted"
        case .limited:
            permissionState = "limited"
        @unknown default:
            permissionState = "prompt"
        }

        return [
            "contacts": permissionState
        ]
    }

    private static func isContactsPermissionGranted() -> Bool {
        switch CNContactStore.authorizationStatus(for: .contacts) {
        case .notDetermined, .restricted, .denied:
            return false
        case .authorized, .limited:
            return true
        @unknown default:
            return false
        }
    }

    // getContact, getContacts, createContact and deleteContact stay synchronous: the bridge queue runs them in the
    // order of the calls, so a read sees the contacts created and deleted before it. Async methods would not keep
    // that order.

    /// Runs `body` with `call` right away when contacts access is granted. Otherwise asks for access first, and then
    /// runs `body` or rejects the call, from the completion of the request; what `body` throws there rejects the call.
    private func withContactsPermission(_ call: CAPPluginCall, _ body: @escaping (CAPPluginCall) throws -> Void) throws {
        if Self.isContactsPermissionGranted() {
            try body(call)
            return
        }
        CNContactStore().requestAccess(for: .contacts) { _, _ in
            guard Self.isContactsPermissionGranted() else {
                call.reject(Self.permissionRequiredMessage)
                return
            }
            do {
                try body(call)
            } catch {
                call.reject(error)
            }
        }
    }

    func getContact(_ call: CAPPluginCall) throws {
        try withContactsPermission(call) { [implementation] call in
            guard let contactId = call.getString("contactId") else {
                throw CAPPluginError("Parameter `contactId` not provided.")
            }

            let projectionInput = GetContactsProjectionInput(call.getObject("projection") ?? JSObject())

            guard let contact = implementation.getContact(contactId, projectionInput) else {
                throw CAPPluginError("Contact not found.")
            }

            call.resolve([
                "contact": contact.getJSObject()
            ])
        }
    }

    func getContacts(_ call: CAPPluginCall) throws {
        try withContactsPermission(call) { [implementation] call in
            let projectionInput = GetContactsProjectionInput(call.getObject("projection") ?? JSObject())

            let contacts = implementation.getContacts(projectionInput)

            var contactsJSArray: JSArray = JSArray()

            for contact in contacts {
                contactsJSArray.append(contact.getJSObject())
            }

            call.resolve([
                "contacts": contactsJSArray
            ])
        }
    }

    func createContact(_ call: CAPPluginCall) throws {
        try withContactsPermission(call) { [implementation] call in
            let contactInput = CreateContactInput.init(call.getObject("contact", JSObject()))

            guard let contactId = implementation.createContact(contactInput) else {
                throw CAPPluginError("Something went wrong.")
            }

            call.resolve([
                "contactId": contactId
            ])
        }
    }

    func deleteContact(_ call: CAPPluginCall) throws {
        try withContactsPermission(call) { [implementation] call in
            guard let contactId = call.getString("contactId") else {
                throw CAPPluginError("Parameter `contactId` not provided.")
            }

            if !implementation.deleteContact(contactId) {
                throw CAPPluginError("Something went wrong.")
            }

            call.resolve()
        }
    }

    /// Presents the system contact picker, which is UIKit, so the method runs on the main actor. Returns
    /// `{ contact }` for the chosen contact; rejects when the picker closes without one or cannot be presented.
    @MainActor
    func pickContact(_ call: CAPPluginCall) async throws -> JSObject {
        if !Self.isContactsPermissionGranted() {
            _ = try? await CNContactStore().requestAccess(for: .contacts)
            guard Self.isContactsPermissionGranted() else {
                throw CAPPluginError(Self.permissionRequiredMessage)
            }
        }
        guard let presenter = Self.topmostPresenter(from: bridge?.viewController) else {
            throw CAPPluginError(Self.noPresenterMessage)
        }

        let outcome = await withCheckedContinuation { (continuation: CheckedContinuation<PickOutcome, Never>) in
            let answer = OnceContinuation(continuation, fallback: PickOutcome.cancelled)
            let delegate = ContactPickerDelegate(answer)
            let picker = CNContactPickerViewController()
            // The picker holds its delegate weakly; the delegate lives as long as the picker, and answers `cancelled`
            // when the picker is released without a choice (for example dismissed by the app).
            picker.delegate = delegate
            objc_setAssociatedObject(picker, &Self.pickerDelegateKey, delegate, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
            presenter.present(picker, animated: true)
            // UIKit sets presentedViewController as soon as it accepts a presentation.
            if presenter.presentedViewController !== picker {
                answer.resume(returning: .refused)
            }
        }

        switch outcome {
        case .picked(let selectedContact):
            let contact = ContactPayload(selectedContact.identifier)
            contact.fillData(selectedContact)
            return [
                "contact": contact.getJSObject()
            ]
        case .cancelled:
            throw CAPPluginError(Self.pickerCancelledMessage)
        case .refused:
            throw CAPPluginError(Self.noPresenterMessage)
        }
    }

    /// The view controller to present from: the last controller in the chain presented over `root`, skipping one that
    /// is being dismissed. Presenting from `root` itself while it already presents a controller fails silently.
    @MainActor
    static func topmostPresenter(from root: UIViewController?) -> UIViewController? {
        var presenter = root
        while let presented = presenter?.presentedViewController, !presented.isBeingDismissed {
            presenter = presented
        }
        return presenter
    }
}

/// How a contact picker closed.
enum PickOutcome {
    case picked(CNContact)
    case cancelled
    /// UIKit refused to present the picker.
    case refused
}

/// Answers a pick with the first thing the picker reports.
final class ContactPickerDelegate: NSObject, CNContactPickerDelegate {
    private let answer: OnceContinuation<PickOutcome>

    init(_ answer: OnceContinuation<PickOutcome>) {
        self.answer = answer
    }

    func contactPicker(_ picker: CNContactPickerViewController, didSelect contact: CNContact) {
        answer.resume(returning: .picked(contact))
    }

    func contactPickerDidCancel(_ picker: CNContactPickerViewController) {
        answer.resume(returning: .cancelled)
    }
}
