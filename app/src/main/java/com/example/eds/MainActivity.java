package com.example.eds;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mazenrashed.printooth.utilities.Printing;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Set;
import java.util.UUID;


import static java.lang.Float.*;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner vendedor, manguera;
    private EditText preciodigitado, placa, kilometraje1;
    private TextView galon, pricepublico, tipoproducto, fechahora, lblprintername, nro_recibo;
    public float corriente = 8200;
    public float acpm = 8115;
    public float extra = 11570;
    String[] opcmangueras = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
    String[] opcvendedores = {"LORENA", "JULIAN", "ADRIANA", "JHON", "MANOLO"};

    //Bluetooth conexion e impresión
    Printing printing;

    Button conectar, desconectar, impresion, btn_copia_anterior, btn_chequear;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    OutputStream outputStream;
    InputStream inputStream;
    Thread thread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy kk:mm:ss");
        String dateTime = simpleDateFormat.format(calendar.getTime());
        fechahora = (TextView) findViewById(R.id.mostrarfechahora); //1er dato
        kilometraje1 = (EditText) findViewById(R.id.kilometraje);
        fechahora.setText(dateTime);

        //Recoger la parte grafica con la parte logica
        // nro_recibo = (TextView) findViewById(R.id.numeroRecibo);
        nro_recibo = (TextView) findViewById(R.id.numeroRecibo);
        vendedor = (Spinner) findViewById(R.id.spinnervendedor); //2do dato
        placa = (EditText) findViewById(R.id.placacarro); //3dato
        preciodigitado = (EditText) findViewById(R.id.precio);
        galon = (TextView) findViewById(R.id.galones);
        tipoproducto = (TextView) findViewById(R.id.producto);
        pricepublico = (TextView) findViewById(R.id.preciopublico); //7mo dato

        manguera = (Spinner) findViewById(R.id.spinnermanguera);

        //Botones de impresion
        conectar = (Button) findViewById(R.id.btn_connect);
        desconectar = (Button) findViewById(R.id.btn_disconnect);
        impresion = (Button) findViewById(R.id.btn_print);
        lblprintername = (TextView) findViewById(R.id.printerName);

        btn_copia_anterior= (Button) findViewById(R.id.btn_reciboanterior);
        btn_chequear= (Button) findViewById(R.id.Check);


        manguera.setOnItemSelectedListener(this);
        vendedor.setOnItemSelectedListener(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, opcmangueras);
        manguera.setAdapter(adapter);

        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, opcvendedores);
        vendedor.setAdapter(adapter1);

         conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    findBluetoothDevice();
                    openBluetoothPrinter();

                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        });
        desconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    disconnectBT();
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        });
        impresion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    printData();
                    //printDataPrinterSmall();

                    placa.setText("");
                    preciodigitado.setText("");
                    galon.setText("");
                    tipoproducto.setText("");
                    pricepublico.setText("");
                    kilometraje1.setText("");

                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        });
        btn_copia_anterior.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    buscarAnterior();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        btn_chequear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    Calcular();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

    }

    private void buscarAnteriorPrinterSmall() {

        AdminSqliteOpenHelper admon= new AdminSqliteOpenHelper(this, "Facturacion", null, 1 );
        SQLiteDatabase BaseDeDatos= admon.getWritableDatabase();
        Cursor fila= BaseDeDatos.rawQuery
                ("select * from informacion where NumeroRecibo = " +
                        "(SELECT MAX(NumeroRecibo)  from informacion)", null);
        if(fila.moveToFirst()) {
            String lastnum = fila.getString(0);
            String lastfechayhora = fila.getString(1);
            String lastvendedor = fila.getString(2);
            String lastplaca = fila.getString(3);
            String lastprecio = fila.getString(4);
            String lastgalon = fila.getString(5);
            String lastproducto = fila.getString(6);
            String lastpreciopub = fila.getString(7);
            String lastmanguera = fila.getString(8);
            BaseDeDatos.close();

            try {
                String mag = "         EDS BELLAVISTA                ";
                mag += "\n";
                mag += "       VILLETA CUNDINAMARCA        ";
                mag += "\n";
                mag += "         Calle 2b #12a             ";
                mag += "\n";
                mag += "       Telefono: 3173445971        ";
                mag += "\n";
                mag += "         NIT: 19375270-0           ";
                mag += "\n";
                mag += "--------------------------------";
                mag += "\n";
                String mag1 = "Nro recibo: ESB_" + lastnum;
                mag1 += "\n";
                mag1 += "\n";
                mag1 += "Fecha-hora: " + lastfechayhora;
                mag1 += "\n";
                mag1 += "\n";
                mag1 += "Producto:     " + lastproducto;
                mag1 += "\n";
                mag1 += "P. publico:   " + lastpreciopub;
                mag1 += "\n";
                mag1 += "Galones:      " + lastgalon;
                mag1 += "\n";
                mag1 += "Manguera:     " + lastmanguera;
                mag1 += "\n";
                mag1 += "Total:        $ " + lastprecio;
                mag1 += "\n";
                mag1 += "\n";
                mag1 += "\n";
                String mag2 = "Vendedor:     " + lastvendedor;
                mag2 += "\n";
                mag2 += "\n";
                mag2 += "Kilometraje:  ";
                mag2 += "\n";
                mag2 += "\n";
                mag2 += "Placa:        " + lastplaca;
                mag2 += "\n";
                mag2 += "\n";
                mag2 += "             COPIA";
                mag2 += "\n";
                mag2 += "--------------------------------";
                mag2 += "\n";
                mag2 += "          FELIZ VIAJE      ";
                mag2 += "\n";
                mag2 += "       GRACIAS POR SU COMPRA";
                mag2 += "\n";
                mag2 += "--------------------------------";
                mag2 += "\n";
                mag2 += "\n";


                outputStream.write(mag.getBytes());
                outputStream.write(mag1.getBytes());
                outputStream.write(mag2.getBytes());


                lblprintername.setText("Imprimiendo copia....");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(this,"no data",Toast.LENGTH_SHORT).show();
        }

    }

    public void numeroMaximo(View View) {
        String data = "";
        AdminSqliteOpenHelper admon = new AdminSqliteOpenHelper(this, "Facturacion", null, 1);
        SQLiteDatabase DataBase = admon.getWritableDatabase();
        Cursor cursor = DataBase.rawQuery("Select max(NumeroRecibo) from informacion", null);
        if (cursor != null) {
            cursor.moveToFirst();
            int index = cursor.getInt(0);
            nro_recibo.setText(String.valueOf(index));
            DataBase.close();
        }
    }

    public void prueba2() {
        int contador = 0;
        AdminSqliteOpenHelper admon = new AdminSqliteOpenHelper(this, "Facturacion", null, 1);
        SQLiteDatabase DataBase = admon.getWritableDatabase();
        Cursor cursor = DataBase.rawQuery("Select max(NumeroRecibo) from informacion", null);
        if (cursor != null) {
            cursor.moveToFirst();
            int index = cursor.getInt(0);

            if (index != 0) {
                contador = index + 1;
                nro_recibo.setText(String.valueOf(contador));
                DataBase.close();
            } else {
                nro_recibo.setText("1");
            }

        }
    }

    public void findBluetoothDevice() {

        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                lblprintername.setText("NO FOUND");
            }
            if (bluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 0);
            }
            Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
            if (pairedDevice.size() > 0) {
                for (BluetoothDevice pairedDev : pairedDevice) {
                    //nombre de la impresora bluetooth
                    if (pairedDev.getName().equals("Printer_E43A")) { //Printer_E43A - MTP-2
                        bluetoothDevice = pairedDev;
                        lblprintername.setText("Bluetooth printer attached: " + pairedDev.getName());
                        break;
                    }
                }
            }
            lblprintername.setText("IMPRESORA CONECTADA");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void openBluetoothPrinter() throws IOException {
        try {
            UUID uuidString = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuidString);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            beginListenData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    void beginListenData() {
        try {
            final Handler handler = new Handler();
            byte delimiter = 10;
            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int byteAvailable = inputStream.available();
                            if (byteAvailable > 0) {
                                byte[] packetByte = new byte[byteAvailable];
                                inputStream.read(packetByte);

                                for (int i = 0; i < byteAvailable; i++) {
                                    byte b = packetByte[i];
                                    if (b == delimiter) {
                                        byte[] encodedByte = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedByte, 0,
                                                encodedByte.length
                                        );
                                        final String data = new String(encodedByte, StandardCharsets.US_ASCII);
                                        readBufferPosition = 0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                lblprintername.setText(data);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            stopWorker = true;
                        }
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void disconnectBT() throws IOException{
        try{
            stopWorker=true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            lblprintername.setText("DESCONECTADO... ");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void printData() throws IOException{

        try{
            prueba2();
            registrar();

            String mag = "                EDS BELLAVISTA                ";
            mag+="\n";
            mag+= "              VILLETA CUNDINAMARCA        ";
            mag+="\n";
            mag+= "                Calle 2b #12a             ";
            mag+="\n";
            mag+= "              Telefono: 3173445971        ";
            mag+="\n";
            mag+= "                NIT: 19375270-0           ";
            mag+="\n";
            mag+= "-----------------------------------------------";
            mag+="\n";
            String mag1="Nro recibo: ESB_" + nro_recibo.getText();
            mag1+="\n";
            mag1+="\n";
            mag1+="Fecha-hora:  " + fechahora.getText();
            mag1+="\n";
            mag1+="\n";
            mag1+="Producto:     " + tipoproducto.getText();
            mag1+="\n";
            mag1+="P. publico:   " + pricepublico.getText();
            mag1+="\n";
            mag1+="Galones:      " + galon.getText();
            mag1+="\n";
            mag1+="Manguera:     " + manguera.getSelectedItem().toString();
            mag1+="\n";
            mag1+="Total:        $ " + preciodigitado.getText();
            mag1+="\n";
            mag1+="\n";
            mag1+="\n";
            String mag2= "Vendedor:     "+ vendedor.getSelectedItem().toString();
            mag2+="\n";
            mag2+="\n";
            mag2+= "Kilometraje:  "+ kilometraje1.getText();
            mag2+="\n";
            mag2+="\n";
            mag2+= "Placa:        "+ placa.getText();
            mag2+="\n";
            mag2+= "-----------------------------------------------";
            mag2+="\n";
            mag2+="                 FELIZ VIAJE      ";
            mag2+="\n";
            mag2+="              GRACIAS POR SU COMPRA";
            mag2+="\n";
            mag2+= "-----------------------------------------------";
            mag2+="\n";
            mag2+="\n";


            outputStream.write(mag.getBytes());
            outputStream.write(mag1.getBytes());
            outputStream.write(mag2.getBytes());


            lblprintername.setText("Imprimiendo....");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void printDataPrinterSmall() throws IOException{

        try{
            prueba2();
            registrar();

            String mag = "         EDS BELLAVISTA                ";
            mag+="\n";
            mag+= "       VILLETA CUNDINAMARCA        ";
            mag+="\n";
            mag+= "         Calle 2b #12a             ";
            mag+="\n";
            mag+= "       Telefono: 3173445971        ";
            mag+="\n";
            mag+= "         NIT: 19375270-0           ";
            mag+="\n";
            mag+= "--------------------------------";
            mag+="\n";
            String mag1="Nro recibo: ESB_" + nro_recibo.getText();
            mag1+="\n";
            mag1+="\n";
            mag1+="Fecha-hora: " + fechahora.getText();
            mag1+="\n";
            mag1+="\n";
            mag1+="Producto:     " + tipoproducto.getText();
            mag1+="\n";
            mag1+="P. publico:   " + pricepublico.getText();
            mag1+="\n";
            mag1+="Galones:      " + galon.getText();
            mag1+="\n";
            mag1+="Manguera:     " + manguera.getSelectedItem().toString();
            mag1+="\n";
            mag1+="Total:        $ " + preciodigitado.getText();
            mag1+="\n";
            mag1+="\n";
            mag1+="\n";
            String mag2= "Vendedor:     "+ vendedor.getSelectedItem().toString();
            mag2+="\n";
            mag2+="\n";
            mag2+= "Kilometraje:  "+ kilometraje1.getText();
            mag2+="\n";
            mag2+="\n";
            mag2+= "Placa:        "+ placa.getText();
            mag2+="\n";
            mag2+= "--------------------------------";
            mag2+="\n";
            mag2+="          FELIZ VIAJE      ";
            mag2+="\n";
            mag2+="       GRACIAS POR SU COMPRA";
            mag2+="\n";
            mag2+= "--------------------------------";
            mag2+="\n";
            mag2+="\n";


            outputStream.write(mag.getBytes());
            outputStream.write(mag1.getBytes());
            outputStream.write(mag2.getBytes());


            lblprintername.setText("Imprimiendo....");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void registrar(){
        //NOMBRE DE LA BASE DE DATOS ES FACTURACION
        AdminSqliteOpenHelper admin= new AdminSqliteOpenHelper(this, "Facturacion", null, 1 );
        SQLiteDatabase BaseDeDatos= admin.getWritableDatabase(); //Abrir base de datos en modo lectura y escritura

        String numerorecibodb= nro_recibo.getText().toString();
        int numerorecibodbinteger= Integer.parseInt(numerorecibodb);
        String fechahoradb= fechahora.getText().toString();
        String vendedordb= vendedor.getSelectedItem().toString();
        String placadb= placa.getText().toString();
        String preciodb= preciodigitado.getText().toString();
        String galonesdb= galon.getText().toString();
        String productodb= tipoproducto.getText().toString();
        String ppublicodb= pricepublico.getText().toString();
        String mangueradb= manguera.getSelectedItem().toString();

        //Verificar que esten todos los campos llenos
        if(!fechahoradb.isEmpty() && !vendedordb.isEmpty() && !numerorecibodb.isEmpty()
                && !preciodb.isEmpty() && !galonesdb.isEmpty() && !productodb.isEmpty()
                && !ppublicodb.isEmpty()) {

            ContentValues registro = new ContentValues();
            registro.put("NumeroRecibo", numerorecibodbinteger);
            registro.put("FechaHora", fechahoradb);
            registro.put("Vendedor", vendedordb);
            registro.put("Placa", placadb);
            registro.put("Precio", preciodb);
            registro.put("Galones", galonesdb);
            registro.put("Producto", productodb);
            registro.put("Ppublico", ppublicodb);
            registro.put("Manguera", mangueradb);

            //Guardarlos en la tabla
            BaseDeDatos.insert("informacion", null, registro);
            BaseDeDatos.close();

            Toast.makeText(this, "REGISTRO EXITOSO",
                    Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "Deben estar llenos todos los campos",
                    Toast.LENGTH_SHORT).show();
        }


    }

    public void buscarAnterior() throws IOException{
        AdminSqliteOpenHelper admon= new AdminSqliteOpenHelper(this, "Facturacion", null, 1 );
        SQLiteDatabase BaseDeDatos= admon.getWritableDatabase();
        Cursor fila= BaseDeDatos.rawQuery
                ("select * from informacion where NumeroRecibo = " +
                        "(SELECT MAX(NumeroRecibo)  from informacion)", null);
        if(fila.moveToFirst()){
            String lastnum= fila.getString(0);
            String lastfechayhora= fila.getString(1);
            String lastvendedor= fila.getString(2);
            String lastplaca= fila.getString(3);
            String lastprecio=fila.getString(4);
            String lastgalon= fila.getString(5);
            String lastproducto= fila.getString(6);
            String lastpreciopub= fila.getString(7);
            String lastmanguera= fila.getString(8);
            BaseDeDatos.close();
            try{
                String mag = "                EDS BELLAVISTA                ";
                mag+="\n";
                mag+= "              VILLETA CUNDINAMARCA        ";
                mag+="\n";
                mag+= "                Calle 2b #12a             ";
                mag+="\n";
                mag+= "              Telefono: 3173445971        ";
                mag+="\n";
                mag+= "                NIT: 19375270-0           ";
                mag+="\n";
                mag+= "-----------------------------------------------";
                mag+="\n";
                String mag1="Nro recibo: ESB_" + lastnum;
                mag1+="\n";
                mag1+="\n";
                mag1+="Fecha, hora:  " + lastfechayhora;
                mag1+="\n";
                mag1+="\n";
                mag1+="Producto:     " + lastproducto;
                mag1+="\n";
                mag1+="P. publico:   " + lastpreciopub;
                mag1+="\n";
                mag1+="Galones:      " + lastgalon;
                mag1+="\n";
                mag1+="Manguera:     " + lastmanguera ;
                mag1+="\n";
                mag1+="Total:        $ " + lastprecio;
                mag1+="\n";
                mag1+="\n";
                mag1+="\n";
                String mag2= "Vendedor:     "+ lastvendedor;
                mag2+="\n";
                mag2+="\n";
                mag2+= "Kilometraje:  ";
                mag2+="\n";
                mag2+="\n";
                mag2+= "Placa:        "+ lastplaca;
                mag2+="\n";
                mag2+="\n";
                mag2+="                    COPIA";
                mag2+="\n";
                mag2+= "-----------------------------------------------";
                mag2+="\n";
                mag2+="                 FELIZ VIAJE      ";
                mag2+="\n";
                mag2+="              GRACIAS POR SU COMPRA";
                mag2+="\n";
                mag2+= "-----------------------------------------------";
                mag2+="\n";
                mag2+="\n";


                outputStream.write(mag.getBytes());
                outputStream.write(mag1.getBytes());
                outputStream.write(mag2.getBytes());
                lblprintername.setText("Imprimiendo copia....");
            }
            catch(Exception e)
            {
                Toast.makeText(this, "couldn´t print", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this,"no data",Toast.LENGTH_SHORT).show();
        }

    }


    //AQUI VA EL METODO CALCULAR
    @SuppressLint("DefaultLocale")
    String roundOffTo3DecPlaces(float val) {

        return String.format("%.3f", val);

    }

    public void Calcular(){

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat= new SimpleDateFormat("dd-MM-yyyy kk:mm:ss ");
        String dateTime= simpleDateFormat.format(calendar.getTime());
        fechahora.setText(dateTime);

        String  precio_string= preciodigitado.getText().toString();

        float precio_float= parseFloat(precio_string);

        String seleccion_manguera= manguera.getSelectedItem().toString();

        if(!precio_string.isEmpty()) {

            if (seleccion_manguera.equals("1") || seleccion_manguera.equals("4") || seleccion_manguera.equals("7") || seleccion_manguera.equals("10")) {

                float galonesacpm = precio_float / acpm;
                String valoracpm = roundOffTo3DecPlaces(galonesacpm);
                galon.setText(valoracpm);
                pricepublico.setText(Float.toString(acpm));
                tipoproducto.setText("ACPM");
            } else if (seleccion_manguera.equals("8") || seleccion_manguera.equals("11")) {

                float galonesextra = precio_float / extra;
                String valorextra = roundOffTo3DecPlaces(galonesextra);
                galon.setText(valorextra);
                pricepublico.setText(Float.toString(extra));
                tipoproducto.setText("EXTRA");
            } else if (seleccion_manguera.equals("2") || seleccion_manguera.equals("3") || seleccion_manguera.equals("5") || seleccion_manguera.equals("6") || seleccion_manguera.equals("9") || seleccion_manguera.equals("12")) {

                float galonescorriente = precio_float / corriente;
                String valorcorriente = roundOffTo3DecPlaces(galonescorriente);
                galon.setText(valorcorriente);
                pricepublico.setText(Float.toString(corriente));
                tipoproducto.setText("CORRIENTE");
            } else if (seleccion_manguera.isEmpty()) {
                Toast.makeText(this, "Debes completar todos los campos", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this, "Digite el precio", Toast.LENGTH_SHORT).show();
        }


    }




    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String text= adapterView.getItemAtPosition(i).toString();
        Toast.makeText(adapterView.getContext(), text, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }




}