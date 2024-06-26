package com.example.piandroid.ui

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.piandroid.R
import com.example.piandroid.controller.LivroAdapter
import com.example.piandroid.controller.Notification
import com.example.piandroid.controller.channelID
import com.example.piandroid.controller.mensagemExtra
import com.example.piandroid.controller.notificatioID
import com.example.piandroid.controller.tituloExtra
import com.example.piandroid.databinding.FragmentCadastroLivroBinding
import com.example.piandroid.model.AppDatabase
import com.example.piandroid.model.Livro
import com.example.piandroid.view.LivroRepository
import com.example.piandroid.view.LivroViewModel
import com.example.piandroid.view.LivroViewModelFactory
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date


class CadastroLivro : Fragment() {

    private var _binding: FragmentCadastroLivroBinding? = null
    private val binding get() = _binding!!
    private lateinit var livroViewModel: LivroViewModel
    private val args: CadastroLivroArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCadastroLivroBinding.inflate(inflater, container, false)

        val dao = AppDatabase.getDatabase(requireContext()).livroDao()
        val repository = LivroRepository(dao)
        val factory = LivroViewModelFactory(repository)
        livroViewModel = ViewModelProvider(this, factory).get(LivroViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNotificationChannel() //cria canal de notificações

        binding.btnCadastrarLivro.setOnClickListener {
            val nome = binding.editNomeLivro.text.toString()
            val paginas = binding.editPaginas.text.toString()
            val paginasLidas = 0 //Por padrão começa em 0
            val favorito = 0 //por padrão não é favorito

            if (nome.isNotEmpty() && paginas.isNotEmpty()) {
                //Livro deve ter uma página (>0)
                if (paginas.toInt() > 0) {
                    val livro = Livro(
                        nome = nome,
                        paginas = paginas.toInt(),
                        paginasLidas = paginasLidas,
                        favorito = favorito
                    )
                    livroViewModel.inserirLivro(livro)
                    Snackbar.make(binding.root, "Livro Cadastrado.", Snackbar.LENGTH_LONG).show()

                    marcarNotificacao()

                    // Navegar de volta à lista após a inserção
                    findNavController().popBackStack()
                } else {
                    Snackbar.make(
                        binding.root,
                        "O livro deve ter pelo menos uma página.",
                        Snackbar.LENGTH_LONG
                    )
                        .show()
                }
            } else {
                Snackbar.make(binding.root, "Preencha todos os campos.", Snackbar.LENGTH_LONG)
                    .show()
            }
        }

    }


    private fun marcarNotificacao() {
        val context = requireContext()
        val intent = Intent(context, Notification::class.java)//classe notification

        var nomeLivro = binding.editNomeLivro.text.toString()
       // var paginasLidas = binding.editPaginasLidas.text.toString()
        var paginas = binding.editPaginas.text.toString()
        val title = "Chegou a hora de ler $nomeLivro!"
        val message = "Não esqueça de atualizar as páginas que você já leu! (0/$paginas)"
        intent.putExtra(tituloExtra, title)//Envia titulo para a classe notification
        intent.putExtra(mensagemExtra, message)//Envia mensagem para a classe notification

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificatioID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = getTime()
        //Seta notificação
        try {
            alarmManager.setExactAndAllowWhileIdle(//Acorda se inativo
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )
            exibeNotificacao(time, title, message)
        } catch (e: SecurityException) {
            Toast.makeText(
                context,
                "Não foi possível agendar a notificação. Por favor conceda as permissões necessárias.",
                Toast.LENGTH_LONG
            ).show()
            pedirPermissaoExactAlarm()
        }
    }


    private fun getTime(): Long {
        // PEGAR NOTIFICACOES DO RDB E ADICIONAR MAIS 24 HORAS
        //val date = LocalDateTime.now().plusDays(1)//data local + 1
        //val date = LocalDateTime.now().dayOfMonth+1//dia + 1
        //val date = LocalTime.now().plusHours(24)//hora atual mais 24 hora atual
        //binding.textView911.setText(date.toString())

        val calendar =
            Calendar.getInstance() // Obtém uma instância do calendário com a data e hora atuais
        calendar.add(Calendar.HOUR_OF_DAY, 24) // Adiciona 24 horas à data e hora atuais

        return calendar.timeInMillis // Retorna o tempo em milissegundos
    }

    private fun pedirPermissaoExactAlarm() {
        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        startActivity(intent)
    }

    private fun exibeNotificacao(time: Long, title: String, message: String) {
        val date = Date(time)
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(context)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)

        AlertDialog.Builder(context)
            .setTitle("Lembrete agendado!")
            .setMessage(
                "Titulo: " + title +
                        "\nMensagem: " + message +
                        "\nLembrete dinamico marcado para: " + dateFormat.format(date) + " " + timeFormat.format(date)
            )
            .setPositiveButton("Okay!") { _, _ -> }
            .show()
    }

    //Canal de notificações
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificações diárias BookUP"
            val desc = "Te lembra a ler seu livro diáriamente."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelID, name, importance)
            channel.description = desc
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}