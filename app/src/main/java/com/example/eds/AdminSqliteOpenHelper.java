package com.example.eds;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64DataException;

import androidx.annotation.Nullable;

public class AdminSqliteOpenHelper extends SQLiteOpenHelper {

    public AdminSqliteOpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase BaseDeDatos) {
        BaseDeDatos.execSQL("create table informacion ( NumeroRecibo String primary key,FechaHora String,Vendedor text," +
                " Placa String, Precio String, Galones String, Producto String, Ppublico String, Manguera String)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
