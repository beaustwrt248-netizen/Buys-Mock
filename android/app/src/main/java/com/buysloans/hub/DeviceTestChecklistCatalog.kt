package com.buysloans.hub

enum class DeviceTestCategory {
    LAPTOP,
    PC,
    CONSOLE,
    PHONE
}

enum class DeviceTestMethod {
    MANUAL_OBSERVATION,
    USER_CONFIRMED,
    SYSTEM_REPORTED
}

data class DeviceTestChecklistItem(
    val id: String,
    val label: String,
    val method: DeviceTestMethod,
    val guidance: String
)

/**
 * Structured Test & Buy checklist definitions.
 *
 * These entries only describe checks that a tester can perform or confirm. They do not claim
 * hardware diagnostics that the app does not actually implement. SYSTEM_REPORTED means a value
 * may be consumed when Android/the device already exposes it; it does not imply active probing.
 */
object DeviceTestChecklistCatalog {
    fun forCategory(category: DeviceTestCategory): List<DeviceTestChecklistItem> = when (category) {
        DeviceTestCategory.LAPTOP -> commonComputerChecks() + listOf(
            item("battery", "Battery condition", DeviceTestMethod.SYSTEM_REPORTED, "Record reported battery condition/capacity when available; otherwise perform a manual charge/discharge observation."),
            item("keyboard_trackpad", "Keyboard and trackpad", DeviceTestMethod.MANUAL_OBSERVATION, "Check representative keys, trackpad movement, click and gestures."),
            item("camera", "Camera", DeviceTestMethod.MANUAL_OBSERVATION, "Open the camera using supported software and confirm image capture/preview."),
            item("charger", "Charging input", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm charging is detected with an appropriate known-good charger.")
        )

        DeviceTestCategory.PC -> commonComputerChecks() + listOf(
            item("boot", "Boot and stability", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm the PC boots and remains stable during the test session."),
            item("display_output", "Display outputs", DeviceTestMethod.MANUAL_OBSERVATION, "Test available display outputs that can be safely connected."),
            item("usb_ports", "USB ports", DeviceTestMethod.MANUAL_OBSERVATION, "Test a representative sample of accessible USB ports with a known-good device.")
        )

        DeviceTestCategory.CONSOLE -> listOf(
            item("power_boot", "Power and boot", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm the console powers on and reaches its normal home/setup screen."),
            item("display", "Display output", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm stable picture output over a supported connection."),
            item("controller", "Controller connection", DeviceTestMethod.MANUAL_OBSERVATION, "Pair/connect a compatible controller and confirm basic input."),
            item("wifi", "Wi-Fi", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm Wi-Fi can be enabled and a network scan/connection works when practical."),
            item("bluetooth", "Bluetooth", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm Bluetooth pairing where the console supports it."),
            item("audio", "Audio", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm audio output through a supported route."),
            item("storage", "Storage availability", DeviceTestMethod.SYSTEM_REPORTED, "Record storage capacity/free-space reported by the console UI when available."),
            item("disc_drive", "Disc drive", DeviceTestMethod.MANUAL_OBSERVATION, "For disc-capable models, confirm insert/eject and recognition with compatible media."),
            item("ports", "External ports", DeviceTestMethod.MANUAL_OBSERVATION, "Inspect and test safely accessible ports appropriate to the model.")
        )

        DeviceTestCategory.PHONE -> listOf(
            item("battery", "Battery condition", DeviceTestMethod.SYSTEM_REPORTED, "Record system-reported battery information when available and note observed charging behaviour."),
            item("display_touch", "Display and touch", DeviceTestMethod.MANUAL_OBSERVATION, "Inspect for damage and verify touch response across the usable screen."),
            item("wifi", "Wi-Fi", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm Wi-Fi can be enabled and can scan/connect when practical."),
            item("bluetooth", "Bluetooth", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm Bluetooth can be enabled and pairing works with a known-good accessory when practical."),
            item("speakers", "Speakers", DeviceTestMethod.MANUAL_OBSERVATION, "Play supported audio and confirm speaker output."),
            item("microphone", "Microphone", DeviceTestMethod.MANUAL_OBSERVATION, "Use a supported recorder/call test and confirm microphone capture."),
            item("cameras", "Cameras", DeviceTestMethod.MANUAL_OBSERVATION, "Open supported camera modes and confirm front/rear preview or capture."),
            item("charging_port", "Charging/data port", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm charging and, where practical, a safe data connection using known-good equipment."),
            item("storage", "Storage availability", DeviceTestMethod.SYSTEM_REPORTED, "Record storage capacity/free-space reported by Android when available."),
            item("biometrics", "Biometric enrolment", DeviceTestMethod.USER_CONFIRMED, "If supported and permitted, confirm the device reports the biometric feature as available; do not retain biometric data."),
            item("buttons", "Physical buttons", DeviceTestMethod.MANUAL_OBSERVATION, "Check power, volume and other accessible physical controls."),
            item("mobile_radio", "Mobile network", DeviceTestMethod.USER_CONFIRMED, "When a suitable SIM/eSIM and network are available, confirm the device can register; otherwise mark not tested.")
        )
    }

    private fun commonComputerChecks() = listOf(
        item("display", "Display", DeviceTestMethod.MANUAL_OBSERVATION, "Inspect the panel/output and confirm a stable image."),
        item("wifi", "Wi-Fi", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm Wi-Fi can be enabled and can scan/connect when practical."),
        item("bluetooth", "Bluetooth", DeviceTestMethod.MANUAL_OBSERVATION, "Confirm Bluetooth can be enabled and pairing works when practical."),
        item("speakers", "Speakers/audio", DeviceTestMethod.MANUAL_OBSERVATION, "Play supported audio and confirm output."),
        item("storage", "Storage availability", DeviceTestMethod.SYSTEM_REPORTED, "Record storage capacity/free-space and health indicators only when the operating system already exposes them."),
        item("ports", "Ports", DeviceTestMethod.MANUAL_OBSERVATION, "Inspect and safely test accessible ports appropriate to the device.")
    )

    private fun item(
        id: String,
        label: String,
        method: DeviceTestMethod,
        guidance: String
    ) = DeviceTestChecklistItem(id, label, method, guidance)
}
