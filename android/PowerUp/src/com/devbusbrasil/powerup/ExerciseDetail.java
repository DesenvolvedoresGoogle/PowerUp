package com.devbusbrasil.powerup;

import com.devbusbrasil.powerup.lib.QRCode;
import com.devbusbrasil.powerup.lib.TrainingItem;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ExerciseDetail extends Activity {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        
        TrainingItem trainingItem = QRCode.GetInstance().getTrainingItem();
        QRCode.GetInstance().setCode(null);
        
        TextView tv = (TextView)findViewById(R.id.trainingText);
        
        if (trainingItem != null) {
            tv.setText(trainingItem.getText());
        }
        else {
            tv.setText("N‹o encontrado");
        }
    }

}
