package com.nxredux.mobile.netplay.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

/** The USB identities of the cable and the Android Open Accessory switch. */
object UsbIds {
    /** The handheld's usblink function: vendor class 0xFF / 0x4E / 1, string "nxlink", any VID/PID. */
    fun isNxlink(cls: Int, subclass: Int, protocol: Int) = cls == 0xFF && subclass == 0x4E && protocol == 1

    /** A phone that switched into accessory mode (Google's AOA IDs). */
    fun isAccessoryMode(vendorId: Int, productId: Int) = vendorId == 0x18D1 && (productId == 0x2D00 || productId == 0x2D01)

    /**
     * A device may get the AOA switch only when none of its interfaces is a peripheral's (HID 3, audio 1,
     * mass storage 8, hub 9, video 14) or a vendor-class game pad's (XInput 0xFF/0x5D, Xbox One GIP 0xFF/0x47); a phone
     * shows MTP/PTP (6), other vendor interfaces (0xFF) or no interface at all. [interfaces] are (class, subclass) pairs.
     */
    fun isAoaCandidate(interfaces: List<Pair<Int, Int>>) =
        interfaces.none { (cls, sub) -> cls in PERIPHERAL_CLASSES || (cls == 0xFF && sub in VENDOR_PAD_SUBCLASSES) }

    /**
     * A handheld before its USB Cable mode starts: the generic Linux-gadget ADB ids (18D1:D002, 18D1:D001) that the Brick
     * ("TRIMUI ADB") and similar handhelds use. It never speaks AOA, so the switch would only cost a pointless permission
     * prompt. ADB-only devices in general stay candidates: a phone with USB debugging looks the same, but it carries its
     * own maker's ids.
     */
    fun isHandheldAdbGadget(vendorId: Int, productId: Int) = vendorId == 0x18D1 && (productId == 0xD002 || productId == 0xD001)

    /** A device Android also shows as an input device (a pad, a keyboard) never gets the AOA switch. */
    fun isInputDevice(vendorId: Int, productId: Int, inputIds: Collection<Pair<Int, Int>>) = (vendorId to productId) in inputIds

    private val PERIPHERAL_CLASSES = setOf(1, 3, 8, 9, 14)
    private val VENDOR_PAD_SUBCLASSES = setOf(0x5D, 0x47)

    const val MANUFACTURER = "NX Redux"
    const val MODEL = "NX Redux Mobile Link"
    /** AOA string indexes 0..5: manufacturer, model, description, version, URI, serial. */
    val AOA_STRINGS = listOf(MANUFACTURER, MODEL, "NX Redux Mobile cable link", "1", "https://github.com/nx-redux/nx-mobile", "NXREDUX0001")

    fun nxlinkInterface(d: UsbDevice): UsbInterface? =
        (0 until d.interfaceCount).map(d::getInterface).firstOrNull { isNxlink(it.interfaceClass, it.interfaceSubclass, it.interfaceProtocol) }

    fun interfaceClasses(d: UsbDevice): List<Pair<Int, Int>> =
        (0 until d.interfaceCount).map(d::getInterface).map { it.interfaceClass to it.interfaceSubclass }

    /** The interface's bulk IN and OUT endpoints, or null. */
    fun bulkPair(iface: UsbInterface): Pair<UsbEndpoint, UsbEndpoint>? {
        val eps = (0 until iface.endpointCount).map(iface::getEndpoint).filter { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK }
        val input = eps.firstOrNull { it.direction == UsbConstants.USB_DIR_IN } ?: return null
        val output = eps.firstOrNull { it.direction == UsbConstants.USB_DIR_OUT } ?: return null
        return input to output
    }

    /** AOA: 51 get protocol (>= 1), 52 the strings, 53 start. False when the device does not speak AOA. */
    fun startAccessory(c: UsbDeviceConnection): Boolean {
        val proto = ByteArray(2)
        if (c.controlTransfer(0xC0, 51, 0, 0, proto, 2, 1000) != 2) return false
        if (((proto[0].toInt() and 0xff) or ((proto[1].toInt() and 0xff) shl 8)) < 1) return false
        AOA_STRINGS.forEachIndexed { i, s ->
            val b = (s + "\u0000").toByteArray(Charsets.UTF_8)
            if (c.controlTransfer(0x40, 52, 0, i, b, b.size, 1000) < 0) return false
        }
        return c.controlTransfer(0x40, 53, 0, 0, null, 0, 1000) >= 0
    }
}
