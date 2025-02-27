import React, { useState } from 'react';
import { Button, Text, View, NativeModules, TextInput } from 'react-native'; // Import TextInput here

const { CalendarModule } = NativeModules;

const App = () => {
  const [connectionStatus, setConnectionStatus] = useState<string>('Not connected');
  const [bluetoothMacAddress, setBluetoothMacAddress] = useState<string>(''); // To store Bluetooth MAC address

  // Open the cash drawer
  const openCashDrawer = () => {
    const pin = 2; // Example PIN, you can use others like POSConst.PIN_ONE, PIN_THREE, etc.
    CalendarModule.openCashDrawer(pin); // Call the native function to open the cash drawer
    setConnectionStatus('Cash drawer opened');
  };

  // Connect to a USB printer for testing
  const connectUSB = () => {
    const pathName = '/dev/usb/lp0'; // Example path for a USB printer
    CalendarModule.connectUSB(pathName); // Connect to a USB printer
    setConnectionStatus('Connecting to USB printer...');
  };

  // Connect to Bluetooth Printer
  const connectBluetooth = () => {
    if (bluetoothMacAddress.trim() === '') {
      setConnectionStatus('Please enter a Bluetooth MAC address');
      return;
    }

    CalendarModule.connectBt(bluetoothMacAddress); // Call the native function to connect via Bluetooth
    setConnectionStatus('Connecting to Bluetooth printer...');
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>{connectionStatus}</Text>

      <Button title="Open Cash Drawer" onPress={openCashDrawer} />
      <Button title="Connect USB Printer" onPress={connectUSB} />
      
      <Text>Enter Bluetooth MAC Address:</Text>
      <TextInput
        style={{
          height: 40,
          borderColor: 'gray',
          borderWidth: 1,
          marginBottom: 20,
          paddingLeft: 8,
          width: 200
        }}
        value={bluetoothMacAddress}
        onChangeText={setBluetoothMacAddress}
        placeholder="MAC Address"
      />
      <Button title="Connect Bluetooth Printer" onPress={connectBluetooth} />
    </View>
  );
};

export default App;
