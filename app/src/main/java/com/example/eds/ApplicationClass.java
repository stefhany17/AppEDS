package com.example.eds;
import android.app.Application;
import com.mazenrashed.printooth.Printooth;

public class ApplicationClass extends Application {

    public void onCreate(){
        super.onCreate();
        Printooth.INSTANCE.init(this);

    }
}
