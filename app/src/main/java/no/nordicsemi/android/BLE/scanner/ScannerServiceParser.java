/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.BLE.scanner;

import java.util.UUID;

import no.nordicsemi.android.BLE.utility.DebugLogger;

/**
 * ScannerServiceParser is responsible to parse scanning data and it check if scanned device has required service in it. It will get advertisement packet and check field name Service Class UUID with
 * value for 128-bit-Service-UUID = {6}
 */
public class ScannerServiceParser {
	private final String TAG = "ScannerServiceParser";
	private final int SERVICE_CLASS_128BIT_UUID = 6;
	private int packetLength = 0;
	private boolean mIsValidSensor = false;
	private String mRequiredUUID;
	private static ScannerServiceParser mParserInstance;

	/**
	 * singleton implementation of ScannerServiceParser
	 */
	public static synchronized ScannerServiceParser getParser() {
		if (mParserInstance == null) {
			mParserInstance = new ScannerServiceParser();
		}
		return mParserInstance;
	}

	public boolean isValidSensor() {
		return mIsValidSensor;
	}

	/**
	 * The method will get advertisement data and required BLE Service UUID as input It will check the existence of 128 bit Service UUID field = {6} For further details on parsing BLE advertisement
	 * packet data see https://developer.bluetooth.org/Pages/default.aspx Bluetooth Core Specifications Volume 3, Part C, and Section 8
	 */
	public void decodeDeviceAdvData(byte[] data, UUID requiredUUID) throws Exception {
		mIsValidSensor = false;
		mRequiredUUID = requiredUUID.toString();
		if (data != null) {
			int fieldLength, fieldName;
			packetLength = data.length;
			for (int index = 0; index < packetLength; index++) {
				fieldLength = data[index];
				if (fieldLength == 0) {
					DebugLogger.d(TAG, "index: " + index + " No more data exist in Advertisement packet");
					return;
				}
				fieldName = data[++index];
				DebugLogger.d(TAG, "fieldName: " + fieldName + " Filed Length: " + fieldLength);

				if (fieldName == SERVICE_CLASS_128BIT_UUID) {
					DebugLogger.d(TAG, "index: " + index + " Service class 128 bit UUID exist");
					decodeService128BitUUID(data, index + 1, fieldLength - 1);
					index += fieldLength - 1;
				} else {
					// Other Field Name						
					index += fieldLength - 1;
				}
			}
		} else {
			DebugLogger.d(TAG, "data is null!");
			return;
		}
	}

	/**
	 * check for required Service UUID inside device
	 */
	private void decodeService128BitUUID(byte[] data, int startPosition, int serviceDataLength) throws Exception {
		DebugLogger.d(TAG, "StartPosition: " + startPosition + " Data length: " + serviceDataLength);
		String serviceUUID = Integer.toHexString(data[startPosition + serviceDataLength - 3]) + Integer.toHexString(data[startPosition + serviceDataLength - 4]);
		String requiredUUID = mRequiredUUID.substring(4, 8);
		DebugLogger.d(TAG, "DeviceServiceUUID: " + serviceUUID + " Required UUID: " + requiredUUID);

		if (serviceUUID.equals(requiredUUID)) {
			DebugLogger.d(TAG, "Service UUID: " + serviceUUID);
			DebugLogger.d(TAG, "Required service exist!");
			mIsValidSensor = true;
		}
	}
}
