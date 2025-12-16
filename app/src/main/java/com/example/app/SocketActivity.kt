package com.example.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class SocketActivity : AppCompatActivity() {
    private var log_tag: String = "MY_LOG_TAG"
    private lateinit var tvSockets: TextView
    private lateinit var btnStartClient: Button
    private lateinit var btnStartServer: Button
    private lateinit var handler: Handler

    // Переменные для управления потоками
    private var serverThread: Thread? = null
    private var clientThread: Thread? = null
    private var isServerRunning = false
    private var isClientRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket)

        tvSockets = findViewById(R.id.tvsoket)
        btnStartClient = findViewById(R.id.btnStartClient)
        btnStartServer = findViewById(R.id.btnStartServer)
        handler = Handler(Looper.getMainLooper())
        setupButtonListeners()
    }
    private fun setupButtonListeners() {
        btnStartServer.setOnClickListener {
            if (!isServerRunning) {
                Log.d(log_tag, "Starting server...")
                startServer()
                //btnStartServer.text = "Stop Server"
            } else {
                Log.d(log_tag, "Stopping server...")
                stopServer()
                //btnStartServer.text = "Start Server"
            }
        }

        btnStartClient.setOnClickListener {
            if (!isClientRunning) {
                Log.d(log_tag, "Starting client...")
                startClient()
                btnStartClient.text = "Stop Client"
            } else {
                Log.d(log_tag, "Stopping client...")
                stopClient()
                btnStartClient.text = "Start Client"
            }
        }
    }

    fun startServer() {
        isServerRunning = true
        serverThread = Thread {
            Log.d(log_tag, "[SERVER THREAD] Server thread started")
            try {
                val context = ZContext()
                val socket = context.createSocket(SocketType.REP)
                socket.bind("tcp://0.0.0.0:5555")
                Log.d(log_tag, "[SERVER] Bound to tcp://0.0.0.0:5555")

                var counter = 0

                while (isServerRunning) {
                    try {
                        val requestBytes = socket.recv(ZMQ.NOBLOCK)
                        if (requestBytes != null) {
                            val request = String(requestBytes, ZMQ.CHARSET)
                            counter++
                            Log.d(log_tag, "[SERVER] Received: $request (total: $counter)")

                            handler.post {
                                tvSockets.text = "Получено: $request\n Счётчик: $counter"
                            }

                            val response = "Hello from Android Server! Count: $counter"
                            socket.send(response.toByteArray(ZMQ.CHARSET), 0)
                            Log.d(log_tag, "[SERVER] Sent response: $response")
                        }

                        Thread.sleep(100)
                    } catch (e: Exception) {
                        Log.e(log_tag, "[SERVER] Error in loop: ${e.message}")
                    }
                }

                socket.close()
                context.close()
                Log.d(log_tag, "[SERVER] Socket and context closed")
            } catch (e: Exception) {
                Log.e(log_tag, "[SERVER] Setup error: ${e.message}")
            } finally {
                Log.d(log_tag, "[SERVER THREAD] Server thread finished")
            }
        }
        serverThread?.start()
    }

    private fun startClient() {
        isClientRunning = true
        clientThread = Thread {
            Log.d(log_tag, "[CLIENT THREAD] Client thread started")
            val context = ZContext()
            val socket = context.createSocket(SocketType.REQ)

            try {
                val serverAddress = "tcp://172.20.10.3:5555"
                socket.connect(serverAddress)
                Log.d(log_tag, "[CLIENT] Connected to $socket.connect(serverAddress)")

                runOnUiThread {
                    tvSockets.text = "Подключено к $serverAddress\n Ожидаю ответа..."
                }

                var messageCount = 0
                while (isClientRunning && messageCount < 100 && !Thread.currentThread().isInterrupted) {
                    try {
                        val message = "Hello from Android! Count: ${++messageCount}"
                        socket.send(message.toByteArray(ZMQ.CHARSET), 0)
                        Log.d(log_tag, "[CLIENT] Sent: $message")

                        val reply = socket.recv(0)
                        if (reply != null) {
                            val response = String(reply, ZMQ.CHARSET)
                            Log.d(log_tag, "[CLIENT] Received: $response")

                            runOnUiThread {
                                tvSockets.text = "Отправлено: $message\n Получено: $response\nПопытка: $messageCount"
                            }
                        } else {
                            Log.e(log_tag, "[CLIENT] No reply from server")
                            runOnUiThread {
                                tvSockets.text = "Нет ответа от сервера\nПопытка: $messageCount"
                            }
                        }
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        Log.d(log_tag, "[CLIENT] Thread interrupted")
                        break
                    } catch (e: Exception) {
                        Log.e(log_tag, "[CLIENT] Send/receive error: ${e.message}")
                        runOnUiThread {
                            tvSockets.text = "Ошибка: ${e.message}\nПопытка: $messageCount"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(log_tag, "[CLIENT] Setup error: ${e.message}")
                runOnUiThread {
                    tvSockets.text = "Не удалось подключиться: ${e.message}"
                    btnStartClient.text = "Start Client"
                }
            } finally {
                try {
                    socket.close()
                    context.close()
                    Log.d(log_tag, "[CLIENT] Socket and context closed")
                } catch (e: Exception) {
                    Log.e(log_tag, "[CLIENT] Error closing resources: ${e.message}")
                }
                isClientRunning = false
                runOnUiThread {
                    btnStartClient.text = "Start Client"
                    tvSockets.append("\nКлиент остановлен")
                }
                Log.d(log_tag, "[CLIENT THREAD] Client thread finished")
            }
        }
        clientThread?.start()
    }


    fun stopServer() {
        Log.d(log_tag, "stopServer() called")
        isServerRunning = false
        serverThread?.interrupt()
    }

    fun stopClient() {
        Log.d(log_tag, "stopClient() called")
        isClientRunning = false
        clientThread?.interrupt()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(log_tag, "onDestroy: stopping server and client")
        stopServer()
        stopClient()
    }

}