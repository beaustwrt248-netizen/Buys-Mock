package com.buysloans.hub

enum class CheckMethod {
    MANUAL_OBSERVATION,
    USER_ASSISTED_TEST,
    BASIC_SYSTEM_REPORT
}

data class DeviceChecklistSpec(
    val id:String,
    val label:String,
    val guidance:String,
    val method:CheckMethod,
    val optional:Boolean = false
)

object DeviceChecklistProfiles {
    private val shared = listOf(
        DeviceChecklistSpec("power", "Powers on / boots normally", "Confirm the device reaches its normal operating screen without repeated restart or shutdown.", CheckMethod.MANUAL_OBSERVATION),
        DeviceChecklistSpec("display", "Display condition and output", "Inspect for cracks, lines, dead areas, flicker and usable image output. This is an observed test, not a panel-health diagnostic.", CheckMethod.MANUAL_OBSERVATION),
        DeviceChecklistSpec("ports", "Physical ports and connectors", "Inspect fitted ports and test representative connectors where practical. Record any loose, damaged or unavailable ports.", CheckMethod.USER_ASSISTED_TEST),
        DeviceChecklistSpec("wifi", "Wi-Fi connectivity", "Connect to a known network or confirm normal Wi-Fi discovery. Do not infer antenna health beyond the observed result.", CheckMethod.USER_ASSISTED_TEST),
        DeviceChecklistSpec("bluetooth", "Bluetooth connectivity", "Pair or discover a known device where practical. Record only the observed result.", CheckMethod.USER_ASSISTED_TEST),
        DeviceChecklistSpec("speakers", "Speakers / audio output", "Play a short known audio sample and listen for output, distortion or missing channels where applicable.", CheckMethod.USER_ASSISTED_TEST),
        DeviceChecklistSpec("storage", "Storage detected and basic health check", "Confirm expected storage is detected and, where the OS exposes a basic health/status report, record that report. Do not claim SMART or flash-health diagnostics when unavailable.", CheckMethod.BASIC_SYSTEM_REPORT)
    )

    fun forCategory(category:DeviceCategory):List<DeviceChecklistSpec> = when(category) {
        DeviceCategory.LAPTOP -> shared + listOf(
            DeviceChecklistSpec("battery", "Battery condition / charging", "Confirm the battery is detected, accepts charge and reports a plausible level. Record visible swelling or rapid drain if observed; do not estimate unseen cell health.", CheckMethod.BASIC_SYSTEM_REPORT),
            DeviceChecklistSpec("keyboard", "Keyboard and trackpad", "Test a representative set of keys, click/gesture input and obvious sticking or dead zones.", CheckMethod.USER_ASSISTED_TEST),
            DeviceChecklistSpec("camera", "Camera", "Open the camera and confirm a usable image. Record only the observed result.", CheckMethod.USER_ASSISTED_TEST),
            DeviceChecklistSpec("microphone", "Microphone", "Record or monitor a short sample and confirm audible input.", CheckMethod.USER_ASSISTED_TEST)
        )
        DeviceCategory.DESKTOP_PC -> shared + listOf(
            DeviceChecklistSpec("usb", "USB ports", "Test representative fitted USB ports with a known device; record untested ports rather than assuming they work.", CheckMethod.USER_ASSISTED_TEST),
            DeviceChecklistSpec("ethernet", "Ethernet", "Where fitted, confirm link or network access with a known cable.", CheckMethod.USER_ASSISTED_TEST, optional = true),
            DeviceChecklistSpec("gpu_output", "GPU / display outputs", "Confirm usable output from the fitted display path(s) that can be practically tested. Do not infer GPU stress stability from a basic output test.", CheckMethod.USER_ASSISTED_TEST)
        )
        DeviceCategory.CONSOLE -> shared + listOf(
            DeviceChecklistSpec("controller", "Controller pairing / input", "Pair a compatible controller and confirm representative buttons/sticks respond.", CheckMethod.USER_ASSISTED_TEST),
            DeviceChecklistSpec("disc", "Disc drive (if fitted)", "If the model has a disc drive, insert known media and confirm detection/read. Mark N/A for digital-only models.", CheckMethod.USER_ASSISTED_TEST, optional = true),
            DeviceChecklistSpec("hdmi", "HDMI output", "Confirm stable picture and audio over HDMI with known-good cable/display where practical.", CheckMethod.USER_ASSISTED_TEST)
        )
        DeviceCategory.PHONE -> shared + listOf(
            DeviceChecklistSpec("battery", "Battery condition / charging", "Confirm charging, battery detection and plausible level. Record visible swelling, shutdowns or rapid drain if observed; do not claim battery capacity testing without supported data.", CheckMethod.BASIC_SYSTEM_REPORT),
            DeviceChecklistSpec("touch", "Touchscreen", "Test touch across the visible screen including edges and multi-touch where practical.", CheckMethod.USER_ASSISTED_TEST),
            DeviceChecklistSpec("camera", "Front / rear cameras", "Open each fitted camera and confirm a usable image and basic focus response where practical.", CheckMethod.USER_ASSISTED_TEST),
            DeviceChecklistSpec("microphone", "Microphone", "Record or monitor a short sample and confirm audible input.", CheckMethod.USER_ASSISTED_TEST),
            DeviceChecklistSpec("cellular", "SIM / cellular detection where available", "Where a suitable SIM/service is available, confirm detection or registration. Mark N/A when it cannot be safely tested.", CheckMethod.USER_ASSISTED_TEST, optional = true),
            DeviceChecklistSpec("nfc", "NFC scan/read test where supported", "Android only: perform a scan/read test against a known NFC tag. This must remain read-only and must not look up, assign, link, unlink or modify inventory.", CheckMethod.USER_ASSISTED_TEST, optional = true)
        )
        DeviceCategory.OTHER -> shared
    }
}
