import XCTest
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
        for method in plugin.pluginMethods {
            XCTAssertTrue(plugin.responds(to: method.selector), "\(method.name) is not exposed to Objective-C")
        }
    }

    func testBiMap() {
        let map = BiMap<String, Int>(["home": 1, "work": 2], "custom", 0)

        XCTAssertEqual(2, map.getValue("work"))
        XCTAssertEqual("home", map.getKey(1))
        XCTAssertEqual(0, map.getValue("unknown"))
        XCTAssertEqual("custom", map.getKey(99))
    }
}
