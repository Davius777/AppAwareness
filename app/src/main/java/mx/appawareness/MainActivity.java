package mx.appawareness;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.kit.awareness.Awareness;
import com.huawei.hms.kit.awareness.barrier.AmbientLightBarrier;
import com.huawei.hms.kit.awareness.barrier.AwarenessBarrier;
import com.huawei.hms.kit.awareness.barrier.BarrierStatus;
import com.huawei.hms.kit.awareness.barrier.BarrierUpdateRequest;
import com.huawei.hms.kit.awareness.barrier.BehaviorBarrier;
import com.huawei.hms.kit.awareness.capture.AmbientLightResponse;
import com.huawei.hms.kit.awareness.capture.BehaviorResponse;
import com.huawei.hms.kit.awareness.status.AmbientLightStatus;
import com.huawei.hms.kit.awareness.status.BehaviorStatus;
import com.huawei.hms.kit.awareness.status.DetectedBehavior;

public class MainActivity extends AppCompatActivity {
    Context context = null;
    Activity activity = null;
    private String TAG = "Awareness-Activity";
    private ImageView imgLuz;
    private TextView txtLuz;
    private Button btnLuz;
    private ImageView imgActividad;
    private TextView txtActividad;
    private Button btnActividad;
    public final String[] EXTERNAL_PERMS = { Manifest.permission.ACTIVITY_RECOGNITION };

    private PendingIntent pendingIntentLuz;
    private PendingIntent pendingIntentActividad;
    private final float _umbral = 10.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.activity = this;
        this.context = this.getBaseContext();

        imgLuz = (ImageView) findViewById(R.id.imgLuz);
        txtLuz = (TextView) findViewById(R.id.txtLuz);
        btnLuz = (Button) findViewById(R.id.btnLuz);
        btnLuz.setOnClickListener(this::onClick);
        imgActividad = (ImageView) findViewById(R.id.imgActividad);
        txtActividad = (TextView) findViewById(R.id.txtActividad);
        btnActividad = (Button) findViewById(R.id.btnActividad);
        btnActividad.setOnClickListener(this::onClick);
        if (!checkPermisos())
            ActivityCompat.requestPermissions(this, EXTERNAL_PERMS, 0);
        else {
            // Inicialización de barrera de luz.
            final String BARRIER_RECEIVER_ACTION_LUZ = getApplication().getPackageName() + "LIGHT_BARRIER_RECEIVER_ACTION";
            Intent intentBarrier = new Intent(BARRIER_RECEIVER_ACTION_LUZ);
            pendingIntentLuz = PendingIntent.getBroadcast(this,9999,intentBarrier, PendingIntent.FLAG_UPDATE_CURRENT);
            LightBarrierReceiver barrierReceiverLuz = new LightBarrierReceiver();
            registerReceiver(barrierReceiverLuz, new IntentFilter(BARRIER_RECEIVER_ACTION_LUZ));
            // Inicialización de barrera de actividad.
            final String BARRIER_RECEIVER_ACTION_ACTIVIDAD = getApplication().getPackageName() + "BEHAVIOR_BARRIER_RECEIVER_ACTION";
            Intent intent = new Intent(BARRIER_RECEIVER_ACTION_ACTIVIDAD);
            pendingIntentActividad = PendingIntent.getBroadcast(this, 9998, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            BehaviorBarrierReceiver barrierReceiverActividad = new BehaviorBarrierReceiver();
            registerReceiver(barrierReceiverActividad, new IntentFilter(BARRIER_RECEIVER_ACTION_ACTIVIDAD));
            // Se agregan las barreras inicializadas.
            AddBarrier(this);
        }
    }
    // Eventos de cada botón.
    public void onClick(View v){
        try {
            switch (v.getId()) {
                case R.id.btnLuz:
                    ObtenStatusLuz();
                    break;
                case R.id.btnActividad:
                    ObtenActividadPersona();
                    break;
            }
        }
        catch (Exception ex){
            Log.e(TAG, "ERROR.onClick. " + ex.getMessage());
        }
    }
    // Se agregan las barreras asociadas a la intensidad de luz y actividad del usuario.
    private void AddBarrier(Context context) {
        // Barrera para cambio de intensidad de luz.
        final float luxValue = _umbral;                                                 // Umbral de lux.
        AwarenessBarrier lightAboveBarrier = AmbientLightBarrier.above(luxValue);       // Definición del límite que activará la barrera.
            String lightBarrierLabel = "Límite excedido: " + luxValue + " lx";
            BarrierUpdateRequest.Builder builderLuz = new BarrierUpdateRequest.Builder();
            BarrierUpdateRequest requestLuz = builderLuz.addBarrier(lightBarrierLabel, lightAboveBarrier,pendingIntentLuz).build();
            Awareness.getBarrierClient(context).updateBarriers(requestLuz)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Toast.makeText(getApplicationContext(), "Barrera de luz agregada exitosamente", Toast.LENGTH_SHORT).show();
                        Log.i(TAG,"Barrera de luz agregada exitosamente");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG,"AddBarrier.AmbientLightBarrier.getBarrierClient.updateBarriers.addOnFailureListener",e);
                    }
                });
        // Barrera para cambio actividad.
        AwarenessBarrier keepStillBarrier = BehaviorBarrier.keeping(BehaviorBarrier.BEHAVIOR_STILL);
        String behaviorBarrierLabel = "QUIETO";
        BarrierUpdateRequest.Builder builderActividad = new BarrierUpdateRequest.Builder();
        BarrierUpdateRequest requestActividad = builderActividad.addBarrier(behaviorBarrierLabel, keepStillBarrier,pendingIntentActividad).build();
        Awareness.getBarrierClient(context).updateBarriers(requestActividad)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Toast.makeText(getApplicationContext(), "Barrera de actividad agregada exitosamente", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG,"AddBarrier.BehaviorBarrier.getBarrierClient.updateBarriers.addOnFailureListener",e);
                    }
                });
    }

    // Obtiene la cantidad de  luz.
    public void ObtenStatusLuz(){
        try {
            Awareness.getCaptureClient(context)
                    .getLightIntensity()
                    .addOnSuccessListener(new OnSuccessListener<AmbientLightResponse>() {
                        @Override
                        public void onSuccess(AmbientLightResponse ambientLightResponse) {
                            AmbientLightStatus ambientLightStatus = ambientLightResponse.getAmbientLightStatus();
                            txtLuz.setText("Lux: " + ambientLightStatus.getLightIntensity() + " lx");
                            if (ambientLightStatus.getLightIntensity() > _umbral)
                                imgLuz.setImageDrawable(getDrawable(R.drawable.ic_light_black_48dp));
                            else
                                imgLuz.setImageDrawable(getDrawable(R.drawable.ic_not_light_black_48dp));
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e("getCaptureClient.getLightIntensity.addOnFailureListener", e.getMessage(), e);
                        }
                    });
        }
        catch (Exception ex){
            Log.e(TAG, "ERROR.ObtenStatusLuz. " + ex.getMessage());
        }
    }
    // Broadcast asociado al cambio de intensidad de luz.
    class LightBarrierReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BarrierStatus barrierStatus = BarrierStatus.extract(intent);
            switch(barrierStatus.getPresentStatus()) {
                case BarrierStatus.TRUE:
                    txtLuz.setText(barrierStatus.getBarrierLabel());
                    txtLuz.setTextColor(getColor(R.color.Blanco));
                    imgLuz.setImageDrawable(getDrawable(R.drawable.ic_light_black_48dp));
                    break;
                case BarrierStatus.FALSE:
                    txtLuz.setText("Por debajo del límite");
                    txtLuz.setTextColor(getColor(R.color.Negro));
                    imgLuz.setImageDrawable(getDrawable(R.drawable.ic_not_light_black_48dp));
                    break;
                case BarrierStatus.UNKNOWN:
                    txtLuz.setText("Estatus desconocido");
                    txtLuz.setTextColor(getColor(R.color.colorAccent));
                    imgLuz.setImageDrawable(getDrawable(R.drawable.ic_help_black_48dp));
                    break;
            }
        }
    }
    // Broadcast asociado al cambio de actividad.
    class BehaviorBarrierReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BarrierStatus barrierStatus = BarrierStatus.extract(intent);
            String label = barrierStatus.getBarrierLabel();
            switch(barrierStatus.getPresentStatus()) {
                case BarrierStatus.TRUE:
                    imgActividad.setImageDrawable(getDrawable(R.drawable.ic_self_improvement_black_48dp));
                    txtActividad.setText("Barrera: " + label);
                    Log.i(TAG, label + " BehaviorBarrierReceiver.status:true");
                    break;
                case BarrierStatus.FALSE:
                    ObtenActividadPersona();
                    Log.i(TAG, label + " BehaviorBarrierReceiver.status:false");
                    break;
                case BarrierStatus.UNKNOWN:
                    Log.i(TAG, label + " BehaviorBarrierReceiver.status:unknown");
                    break;
            }
        }
    }

    private void ObtenActividadPersona(){
        try {
            Awareness.getCaptureClient(context)
                    .getBehavior()
                    .addOnSuccessListener(new OnSuccessListener<BehaviorResponse>() {
                        @Override
                        public void onSuccess(BehaviorResponse behaviorResponse) {
                            BehaviorStatus behaviorStatus = behaviorResponse.getBehaviorStatus();
                            DetectedBehavior detectedBehavior = behaviorStatus.getMostLikelyBehavior();
                            if(detectedBehavior.getConfidence() > 20)
                                switch (detectedBehavior.getType()){
                                    case BehaviorBarrier.BEHAVIOR_IN_VEHICLE:
                                        imgActividad.setImageDrawable(getDrawable(R.drawable.ic_directions_car_black_48dp));
                                        txtActividad.setText(getString(R.string.transporte));
                                        break;
                                    case BehaviorBarrier.BEHAVIOR_ON_BICYCLE:
                                        imgActividad.setImageDrawable(getDrawable(R.drawable.ic_directions_bike_black_48dp));
                                        txtActividad.setText(getString(R.string.bicicleta));
                                        break;
                                    case BehaviorBarrier.BEHAVIOR_ON_FOOT:
                                        imgActividad.setImageDrawable(getDrawable(R.drawable.ic_transfer_within_a_station_black_48dp));
                                        txtActividad.setText(getString(R.string.pie));
                                        break;
                                    case BehaviorBarrier.BEHAVIOR_RUNNING:
                                        imgActividad.setImageDrawable(getDrawable(R.drawable.ic_directions_run_black_48dp));
                                        txtActividad.setText(getString(R.string.corriendo));
                                        break;
                                    case BehaviorBarrier.BEHAVIOR_STILL:
                                        imgActividad.setImageDrawable(getDrawable(R.drawable.ic_self_improvement_black_48dp));
                                        txtActividad.setText(getString(R.string.quieto));
                                        break;
                                    case BehaviorBarrier.BEHAVIOR_UNKNOWN:
                                        imgActividad.setImageDrawable(getDrawable(R.drawable.ic_help_black_48dp));
                                        txtActividad.setText(getString(R.string.desconocido));
                                        break;
                                    case BehaviorBarrier.BEHAVIOR_WALKING:
                                        imgActividad.setImageDrawable(getDrawable(R.drawable.ic_directions_walk_black_48dp));
                                        txtActividad.setText(getString(R.string.caminando));
                                        break;
                                }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e("ObtenActividadPersona.getCaptureClient.getBehavior.addOnFailureListener", e.getMessage(), e);
                        }
                    });
        }
        catch (Exception ex){
            Log.e(TAG, "ERROR.ObtenActividadPersona. " + ex.getMessage());
        }
    }
    private boolean checkPermisos(){
        int permisoActividad = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) : PackageManager.PERMISSION_GRANTED;
        return permisoActividad == PackageManager.PERMISSION_GRANTED;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull java.lang.String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 0 || grantResults.length == 0)
            Toast.makeText(context, "Los permisos no han sido autorizados exitosamente.", Toast.LENGTH_SHORT).show();
    }
}