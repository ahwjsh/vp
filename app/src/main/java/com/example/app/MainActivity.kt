package com.example.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    private var log_tag : String = "MY_LOG_TAG"
    private lateinit var bGoToCalculatorActivity : Button
    private lateinit var bGoToPlayerActivity : Button
    private lateinit var bGoToLocationActivity : Button
    private lateinit var bGoToSocketActivity: Button
    private lateinit var bGoToSaveLocationActivity : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bGoToCalculatorActivity = findViewById(R.id.go_to_calculator_activity)
        bGoToPlayerActivity = findViewById(R.id.go_to_player_activity)
        bGoToLocationActivity = findViewById(R.id.go_to_location_activity)
        bGoToSocketActivity = findViewById(R.id.go_to_socket_activity)
        bGoToSaveLocationActivity = findViewById(R.id.go_to_save_location_activity)
    }
    override fun onStart() {
        super.onStart()
        Log.d (log_tag, "onStart method")
    }

    override fun onResume() {
        super.onResume()
        Log.d (log_tag, "onResume method")
        bGoToCalculatorActivity.setOnClickListener({
            val randomIntent = Intent(this, CalculatorActivity::class.java)
            startActivity(randomIntent)
        });
        bGoToPlayerActivity.setOnClickListener({
            val randomIntent = Intent(this, PlayerActivity::class.java)
            startActivity(randomIntent)
        });
        bGoToLocationActivity.setOnClickListener({
            val randomIntent = Intent(this, LocationActivity::class.java)
            startActivity(randomIntent)
        });
        bGoToSocketActivity.setOnClickListener({
            val randomIntent = Intent(this, SocketActivity::class.java)
            startActivity(randomIntent)
        });
//        bGoToSaveLocationActivity.setOnClickListener({
//            val randomIntent = Intent(this, SaveLocationActivity::class.java)
//            startActivity(randomIntent)
//        });

//        var counter : Int = 0
//        for( i in 0..10000000){
//            counter++
//            Log.d (log_tag, counter.toString())
//        }
    }

    override fun onPause() {
        super.onPause()
        Log.d (log_tag, "onPause method")
    }

    override fun onStop() {
        super.onStop()
        Log.d (log_tag, "onStop method")
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d (log_tag, "onDestroy method")
    }
}