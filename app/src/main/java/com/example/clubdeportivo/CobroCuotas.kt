package com.example.clubdeportivo
import android.app.DatePickerDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CobroCutas : AppCompatActivity() {
    private lateinit var dbHelper: MiBaseDeDatosHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var calendarInicio: Calendar
    private lateinit var calendarFin: Calendar
    private lateinit var calendarPago: Calendar
    private lateinit var editTextInicio: EditText
    private lateinit var editTextFin: EditText
    private lateinit var editTextPago: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cobro_cuotas)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        calendarInicio = Calendar.getInstance()
        calendarFin = Calendar.getInstance()
        calendarPago = Calendar.getInstance()

        editTextInicio = findViewById(R.id.editTextInicioCuota)
        editTextFin = findViewById(R.id.editTextFinCuota)
        editTextPago = findViewById(R.id.editTextFechaPago)

        editTextInicio.setOnClickListener {
            showDatePickerDialogInicio()
        }
        editTextFin.setOnClickListener {
            showDatePickerDialogFin()
        }
        editTextPago.setOnClickListener {
            showDatePickerDialogPago()
        }

        //radio button rbtEfectivo por defecto
        val radioButtonToCheck: RadioButton = findViewById(R.id.rbtEfectivo)
        radioButtonToCheck.isChecked = true

        val socios = ArrayList<Socio>()
        var selectedSocio: Socio? = null

        dbHelper = MiBaseDeDatosHelper(this)
        db = dbHelper.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM socio", null)
        while (cursor.moveToNext()) {
            val nroSocio = cursor.getInt(0)
            val nombre = cursor.getString(1)
            val dni = cursor.getInt(2)
            socios.add(Socio(nroSocio, nombre, dni))
        }
        cursor.close()
        db.close()

        /*
        * Configuración del AutoCompleteTextView
         */
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        val sociosInfo = socios.map { "${it.dni} - ${it.nombre}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sociosInfo)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedInfo = parent.getItemAtPosition(position) as String
            selectedSocio = socios.find { "${it.dni} - ${it.nombre}" == selectedInfo }
        }
        autoCompleteTextView.validator = object : AutoCompleteTextView.Validator {
            override fun isValid(text: CharSequence?): Boolean {
                return sociosInfo.contains(text.toString())
            }
            override fun fixText(invalidText: CharSequence?): CharSequence {
                return ""
            }
        }
        // Validar el texto cuando el campo pierda el foco
        autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                autoCompleteTextView.performValidation()
            }
        }

        val btnCobrarCuota: Button = findViewById(R.id.btnCobrar)
        btnCobrarCuota.setOnClickListener {
            val cuota = Cuota()
            cuota.nSocio = selectedSocio?.nroSocio
            cuota.monto = findViewById<EditText>(R.id.editTextMonto).text.toString().toInt()
            cuota.fechaPago = editTextPago.text.toString()
            cuota.fechaInicio = editTextInicio.text.toString()
            cuota.vencimiento = editTextFin.text.toString()

            val radioGroup: RadioGroup = findViewById(R.id.rdgMetodoPago)

            val selectedRadioButtonText: String =
                (radioGroup.findViewById<RadioButton>(radioGroup.checkedRadioButtonId))?.text.toString()
            if (selectedRadioButtonText == "Efectivo") {
                cuota.metodoPago = MetodoPago.EFECTIVO.value
            } else {
                cuota.metodoPago = MetodoPago.TARJETA.value
            }

            dbHelper = MiBaseDeDatosHelper(this)
            db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("NSocio", selectedSocio?.nroSocio)
                put("Monto", cuota.monto)
                put("FechaPago", cuota.fechaPago)
                put("MetodoPago", cuota.metodoPago)
                put("FechaInicio", cuota.fechaInicio)
                put("Vencimiento", cuota.vencimiento)
            }
            val newRowId = db.insert("cuotaSocio", null, values)
            db.close()

            if (newRowId != -1L) {
                metodos.mostrarAlerta(
                    this,
                    "Cobro de cuota",
                    "Cobro de cuota realizado correctamente."
                )
            } else {
                metodos.mostrarAlerta(
                    this,
                    "Error",
                    "Hubo un error al insertar en la base de datos."
                )
            }
            val btnVerComprobante = findViewById<Button>(R.id.btnVerComprobante)
            btnVerComprobante.isEnabled = true
            btnCobrarCuota.isEnabled = false
        }

        val btnNuevoCobro = findViewById<Button>(R.id.btnNuevoCobro)
        val btnVerComprobante = findViewById<Button>(R.id.btnVerComprobante)
        btnNuevoCobro.setOnClickListener {
            // Limpia los campos del formulario
            findViewById<EditText>(R.id.editTextMonto).setText("")
            editTextFin.setText("")
            editTextInicio.setText("")
            editTextPago.setText("")
            autoCompleteTextView.setText("")
            btnVerComprobante.isEnabled = false
            btnCobrarCuota.isEnabled = true
            radioButtonToCheck.isChecked = true
        }
    }

    private fun showDatePickerDialogInicio() {
        val currentYear = calendarInicio.get(Calendar.YEAR)
        val currentMonth = calendarInicio.get(Calendar.MONTH)
        val currentDay = calendarInicio.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendarInicio.set(Calendar.YEAR, year)
                calendarInicio.set(Calendar.MONTH, month)
                calendarInicio.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDate = dateFormat.format(calendarInicio.time)
                editTextInicio.setText(selectedDate)
            },
            currentYear,
            currentMonth,
            currentDay
        )
        datePickerDialog.show()
    }
    private fun showDatePickerDialogFin() {
        val currentYear = calendarFin.get(Calendar.YEAR)
        val currentMonth = calendarFin.get(Calendar.MONTH)
        val currentDay = calendarFin.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendarFin.set(Calendar.YEAR, year)
                calendarFin.set(Calendar.MONTH, month)
                calendarFin.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDate = dateFormat.format(calendarFin.time)
                editTextFin.setText(selectedDate)
            },
            currentYear,
            currentMonth,
            currentDay
        )
        datePickerDialog.show()
    }
    private fun showDatePickerDialogPago() {
        val currentYear = calendarPago.get(Calendar.YEAR)
        val currentMonth = calendarPago.get(Calendar.MONTH)
        val currentDay = calendarPago.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendarPago.set(Calendar.YEAR, year)
                calendarPago.set(Calendar.MONTH, month)
                calendarPago.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDate = dateFormat.format(calendarPago.time)
                editTextPago.setText(selectedDate)
            },
            currentYear,
            currentMonth,
            currentDay
        )
        datePickerDialog.show()
    }

}