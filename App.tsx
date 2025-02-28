import React, { useState, useEffect } from 'react';
import { Button, Text, View, NativeModules, TextInput, NativeEventEmitter, StyleSheet, PermissionsAndroid, Alert } from 'react-native';

const { CalendarModule } = NativeModules;
const printerEmitter = new NativeEventEmitter(CalendarModule);

const App = () => {
  const [connectionStatus, setConnectionStatus] = useState<string>('Not connected');
  const [bluetoothMacAddress, setBluetoothMacAddress] = useState<string>('');
  const [ipAddress, setIpAddress] = useState<string>('');

  useEffect(() => {
    // Check and request Bluetooth permissions when the app starts
    const requestBluetoothPermissions = async () => {
      try {
        const granted = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        ]);

        // Check if all Bluetooth permissions are granted
        if (
          granted['android.permission.BLUETOOTH_SCAN'] === PermissionsAndroid.RESULTS.GRANTED &&
          granted['android.permission.BLUETOOTH_CONNECT'] === PermissionsAndroid.RESULTS.GRANTED &&
          granted['android.permission.ACCESS_FINE_LOCATION'] === PermissionsAndroid.RESULTS.GRANTED
        ) {
          console.log('Bluetooth permissions granted');
        } else {
          // If any permission is denied, show an alert
          Alert.alert(
            'Permissions Required',
            'This app requires Bluetooth permissions to function properly. Please enable Bluetooth and location permissions.',
            [{ text: 'OK' }]
          );
        }
      } catch (err) {
        console.warn(err);
      }
    };

    requestBluetoothPermissions();

    // Set up event listeners for printer connection events
    const connectionSubscriptions = [
      printerEmitter.addListener('onPrinterConnected', (event) => {
        console.log('Printer connected:', event);
        setConnectionStatus('Connected successfully');
      }),
      printerEmitter.addListener('onPrinterConnectionFailed', (event) => {
        console.log('Connection failed:', event);
        setConnectionStatus(`Connection failed: ${event.message}`);
      }),
      printerEmitter.addListener('onPrinterConnectionInterrupted', (event) => {
        console.log('Connection interrupted:', event);
        setConnectionStatus(`Connection interrupted: ${event.message}`);
      }),
      printerEmitter.addListener('onPrinterSendFailed', (event) => {
        console.log('Send failed:', event);
        setConnectionStatus(`Send failed: ${event.message}`);
      }),
      printerEmitter.addListener('onUsbDetached', () => {
        console.log('USB device detached');
        setConnectionStatus('USB device detached');
      }),
      printerEmitter.addListener('onUsbAttached', () => {
        console.log('USB device attached');
        setConnectionStatus('USB device attached');
      }),
    ];

    // Cleanup function
    return () => {
      connectionSubscriptions.forEach(subscription => subscription.remove());
    };
  }, []);

  const checkConnection = () => {
    CalendarModule.isConnected((error: string, connected: boolean) => {
      if (error) {
        setConnectionStatus(`Error checking connection: ${error}`);
      } else {
        setConnectionStatus(connected ? 'Printer is connected' : 'Printer is not connected');
      }
    });
  };

  const openCashDrawer = () => {
    const pin = 2;
    CalendarModule.openCashDrawer(pin, (error: string, result: string) => {
      if (error) {
        setConnectionStatus(`Error opening cash drawer: ${error}`);
      } else {
        setConnectionStatus(`Cash drawer: ${result}`);
      }
    });
  };

  const connectUSB = () => {
    const pathName = '/dev/usb/lp0';
    setConnectionStatus('Connecting to USB printer...');
    CalendarModule.connectUSB(pathName, (error: string, result: string) => {
      if (error) {
        setConnectionStatus(`USB connection error: ${error}`);
      } else {
        setConnectionStatus(`USB: ${result}`);
      }
    });
  };

  const connectBluetooth = () => {
    if (bluetoothMacAddress.trim() === '') {
      setConnectionStatus('Please enter a Bluetooth MAC address');
      return;
    }

    setConnectionStatus('Connecting to Bluetooth printer...');
    CalendarModule.connectBt(bluetoothMacAddress, (error: string, result: string) => {
      if (error) {
        setConnectionStatus(`Bluetooth connection error: ${error}`);
      } else {
        setConnectionStatus(`Bluetooth: ${result}`);
      }
    });
  };

  const connectNetwork = () => {
    if (ipAddress.trim() === '') {
      setConnectionStatus('Please enter an IP address');
      return;
    }

    setConnectionStatus('Connecting to network printer...');
    CalendarModule.connectNet(ipAddress, (error: string, result: string) => {
      if (error) {
        setConnectionStatus(`Network connection error: ${error}`);
      } else {
        setConnectionStatus(`Network: ${result}`);
      }
    });
  };

  const closeConnection = () => {
    CalendarModule.closeConnection((error: string, result: string) => {
      if (error) {
        setConnectionStatus(`Error closing connection: ${error}`);
      } else {
        setConnectionStatus('Connection closed');
      }
    });
  };

  return (
    <View style={styles.container}>
      <Text style={styles.statusText}>{connectionStatus}</Text>

      <View style={styles.buttonContainer}>
        <Button title="Check Connection" onPress={checkConnection} />
        <Button title="Open Cash Drawer" onPress={openCashDrawer} />
      </View>

      <View style={styles.section}>
        <Button title="Connect USB Printer" onPress={connectUSB} />
      </View>

      <View style={styles.section}>
        <Text>Bluetooth MAC Address:</Text>
        <TextInput
          style={styles.input}
          value={bluetoothMacAddress}
          onChangeText={setBluetoothMacAddress}
          placeholder="00:11:22:33:44:55"
        />
        <Button title="Connect Bluetooth Printer" onPress={connectBluetooth} />
      </View>

      <View style={styles.section}>
        <Text>Network Printer IP:</Text>
        <TextInput
          style={styles.input}
          value={ipAddress}
          onChangeText={setIpAddress}
          placeholder="192.168.1.100"
          keyboardType="numeric"
        />
        <Button title="Connect Network Printer" onPress={connectNetwork} />
      </View>

      <View style={styles.section}>
        <Button title="Close Connection" onPress={closeConnection} color="#ff5252" />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  statusText: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    marginBottom: 20,
  },
  section: {
    width: '100%',
    marginBottom: 20,
    alignItems: 'center',
  },
  input: {
    height: 40,
    borderColor: 'gray',
    borderWidth: 1,
    marginVertical: 10,
    paddingHorizontal: 10,
    width: '100%',
    borderRadius: 5,
  },
});

export default App;
