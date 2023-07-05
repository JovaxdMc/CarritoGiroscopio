package com.example.controlgiroscopio;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    private Button btnConnectBluetooth;
    private Button btnUp;
    private Button btnDown;
    private Button btnLeft;
    private Button btnRight;
    private Button btnDetener;

    private TextView txtX;
    private TextView txtY;
    private TextView txtZ;

    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private List<BluetoothDevice> deviceList;

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    String msg;

    private Handler repeatHandler;
    private boolean repeatFlag = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID para SPP (Serial Port Profile)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtener referencias de los elementos de la interfaz
        btnConnectBluetooth = findViewById(R.id.btnConnectBluetooth);
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnDetener = findViewById(R.id.btnDetener);
        txtX = findViewById(R.id.txtX);
        txtY = findViewById(R.id.txtY);
        txtZ = findViewById(R.id.txtZ);

        // Inicializar el adaptador Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Verificar si el dispositivo soporta Bluetooth
        if (bluetoothAdapter == null) {
            // El dispositivo no soporta Bluetooth
            // Realizar alguna acción aquí, si es necesario
        }

        // Verificar si los permisos de Bluetooth están concedidos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Si no se tienen los permisos, solicitarlos al usuario
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_PERMISSION);
        }

        // Verificar si el Bluetooth está habilitado
        if (!bluetoothAdapter.isEnabled()) {
            // Si el Bluetooth está deshabilitado, solicitar al usuario que lo habilite
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Asignar acciones a los botones
        btnConnectBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectBluetooth();
            }
        });

        // Inicializar el SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Verificar si el dispositivo tiene acelerómetro
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // Obtener el acelerómetro del dispositivo
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            // El dispositivo no tiene acelerómetro
            // Realizar alguna acción o mostrar un mensaje de error
        }

        // Inicializar el color inicial de los botones en blanco
        btnUp.setBackgroundColor(Color.WHITE);
        btnDown.setBackgroundColor(Color.WHITE);
        btnLeft.setBackgroundColor(Color.WHITE);
        btnRight.setBackgroundColor(Color.WHITE);

        // Asignar acciones a los botones
        btnConnectBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectBluetooth();
            }
        });

        // Configurar el evento de clic del botón de detener
        btnDetener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand("5");
            }
        });

        // Configurar el evento de clic del botón de dirección "Arriba"
        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand(msg);
            }
        });

        // Configurar el evento de clic del botón de dirección "Abajo"
        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand(msg);
            }
        });

        // Configurar el evento de clic del botón de dirección "Izquierda"
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand(msg);
            }
        });

        // Configurar el evento de clic del botón de dirección "Derecha"
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand(msg);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar el listener del acelerómetro
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detener la escucha del acelerómetro al pausar la actividad
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Obtener los valores de los ejes X, Y, Z del acelerómetro
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Redondear los valores de los ejes
        x = Math.round(x * 100) / 100.0f;
        y = Math.round(y * 100) / 100.0f;
        z = Math.round(z * 100) / 100.0f;

        // Actualizar los TextViews con los valores de los ejes
        txtX.setText("X: " + String.valueOf(x));
        txtY.setText("Y: " + String.valueOf(y));
        txtZ.setText("Z: " + String.valueOf(z));

        // Verificar la inclinación del teléfono y cambiar el color de los botones según los valores del acelerómetro
        if (y > 8) {
            btnDown.setBackgroundColor(Color.GREEN); // Si el valor de x es mayor a 1, cambiar color del botón "Arriba" a verde
            msg="3";
            btnDown.performClick();

        } else {
            btnDown.setBackgroundColor(Color.WHITE); // De lo contrario, cambiar color del botón "Arriba" a blanco
        }

        if (y < -8) {
            btnUp.setBackgroundColor(Color.GREEN); // Si el valor de x es menor a -1, cambiar color del botón "Abajo" a verde
            msg="1";
            btnUp.performClick();


        } else {
            btnUp.setBackgroundColor(Color.WHITE); // De lo contrario, cambiar color del botón "Abajo" a blanco
        }

        if (x > 8) {
            btnLeft.setBackgroundColor(Color.GREEN); // Si el valor de y es mayor a 1, cambiar color del botón "Izquierda" a verde
            btnLeft.performClick();
            msg="2";
        } else {
            btnLeft.setBackgroundColor(Color.WHITE); // De lo contrario, cambiar color del botón "Izquierda" a blanco
        }

        if (x < -8) {
            btnRight.setBackgroundColor(Color.GREEN); // Si el valor de y es menor a -1, cambiar color del botón "Derecha" a verde
            btnRight.performClick();
            msg="4";
        } else {
            btnRight.setBackgroundColor(Color.WHITE); // De lo contrario, cambiar color del botón "Derecha" a blanco
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No es necesario implementar nada aquí para este ejemplo
    }

    private void connectBluetooth() {
        // Obtener los dispositivos Bluetooth emparejados
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceList = new ArrayList<>();

        // Verificar si hay dispositivos emparejados
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(device);
            }

            // Mostrar un cuadro de diálogo con la lista de dispositivos emparejados
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Selecciona un dispositivo");
            builder.setCancelable(true);

            // Obtener los nombres de los dispositivos emparejados
            CharSequence[] deviceNames = new CharSequence[deviceList.size()];
            for (int i = 0; i < deviceList.size(); i++) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                deviceNames[i] = deviceList.get(i).getName();
            }

            // Configurar el cuadro de diálogo con los nombres de los dispositivos emparejados
            builder.setItems(deviceNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Obtener el dispositivo seleccionado
                    BluetoothDevice selectedDevice = deviceList.get(which);

                    // Iniciar una conexión Bluetooth con el dispositivo seleccionado
                    try {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();
                        Toast.makeText(MainActivity.this, "Conexión establecida", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error al establecer la conexión", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Mostrar el cuadro de diálogo
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            Toast.makeText(this, "No hay dispositivos emparejados", Toast.LENGTH_SHORT).show();
        }
    }
    private void sendBluetoothCommand(String command) {
        if (outputStream != null) {
            try {
                outputStream.write(command.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al enviar el comando Bluetooth", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay conexión Bluetooth establecida", Toast.LENGTH_SHORT).show();
        }
    }

}
