import XCTest
import UIKit
import Contacts
import ContactsUI
@testable import ContactsPlugin

class ContactsTests: XCTestCase {

    func testPluginRegistration() {
        let plugin = ContactsPlugin()

        XCTAssertEqual("ContactsPlugin", plugin.identifier)
        XCTAssertEqual("Contacts", plugin.jsName)
        XCTAssertEqual(
            ["checkPermissions", "requestPermissions", "getContact", "getContacts", "createContact", "deleteContact", "pickContact"],
            plugin.pluginMethods.map { $0.name }
        )
        XCTAssertTrue(plugin.pluginMethods.allSatisfy { $0.returnType == .promise })
    }

    func testPermissionResultMapsEveryAuthorizationStatus() {
        XCTAssertEqual(ContactsPlugin.permissionResult(.notDetermined)["contacts"] as? String, "prompt")
        XCTAssertEqual(ContactsPlugin.permissionResult(.restricted)["contacts"] as? String, "denied")
        XCTAssertEqual(ContactsPlugin.permissionResult(.denied)["contacts"] as? String, "denied")
        XCTAssertEqual(ContactsPlugin.permissionResult(.authorized)["contacts"] as? String, "granted")
        if #available(iOS 18, *) {
            XCTAssertEqual(ContactsPlugin.permissionResult(.limited)["contacts"] as? String, "limited")
        }
    }

    @MainActor
    func testTopmostPresenterIsTheRootWhenNothingIsPresented() {
        let root = UIViewController()
        XCTAssertTrue(ContactsPlugin.topmostPresenter(from: root) === root)
        XCTAssertNil(ContactsPlugin.topmostPresenter(from: nil))
    }

    @MainActor
    func testPickerDelegateAnswersTheFirstReportOnly() async {
        let picker = CNContactPickerViewController()
        let outcome = await withCheckedContinuation { (continuation: CheckedContinuation<PickOutcome, Never>) in
            let delegate = ContactPickerDelegate(OnceContinuation(continuation, fallback: .refused))
            delegate.contactPickerDidCancel(picker)
            delegate.contactPicker(picker, didSelect: CNContact())
        }
        guard case .cancelled = outcome else {
            return XCTFail("expected the cancellation, got \(outcome)")
        }
    }

    @MainActor
    func testPickerDelegateReleasedWithoutAReportAnswersTheFallback() async {
        let outcome = await withCheckedContinuation { (continuation: CheckedContinuation<PickOutcome, Never>) in
            _ = ContactPickerDelegate(OnceContinuation(continuation, fallback: .cancelled))
        }
        guard case .cancelled = outcome else {
            return XCTFail("expected the cancellation, got \(outcome)")
        }
    }

    @MainActor
    func testPickerDelegateAnswersTheSelectedContact() async {
        let contact = CNMutableContact()
        contact.givenName = "Ada"
        let outcome = await withCheckedContinuation { (continuation: CheckedContinuation<PickOutcome, Never>) in
            let delegate = ContactPickerDelegate(OnceContinuation(continuation, fallback: .cancelled))
            delegate.contactPicker(CNContactPickerViewController(), didSelect: contact)
        }
        guard case .picked(let picked) = outcome else {
            return XCTFail("expected the contact, got \(outcome)")
        }
        XCTAssertEqual(picked.givenName, "Ada")
    }

    func testBiMap() {
        let map = BiMap<String, Int>(["home": 1, "work": 2], "custom", 0)

        XCTAssertEqual(2, map.getValue("work"))
        XCTAssertEqual("home", map.getKey(1))
        XCTAssertEqual(0, map.getValue("unknown"))
        XCTAssertEqual("custom", map.getKey(99))
    }
}
