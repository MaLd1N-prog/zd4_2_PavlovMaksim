package com.example.zd4_pavlovmaksim

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.UUID

private const val ARG_CRIME_ID = "crime_id"
private const val REQUEST_CONTACT = 1
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val KEY_PHONE = "suspect_phone"
private const val KEY_CONTACT_ID = "contact_id"

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var suspectButton: Button
    private lateinit var reportButton: Button
    private lateinit var callButton: Button

    private var pendingSuspect: String? = null
    private var suspectPhone: String = ""
    private var contactId: Long = -1L

    // Задание 4.2: runtime-разрешение на чтение контактов (для номера телефона)
    private val requestContactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && contactId >= 0) loadPhoneNumber()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        solvedCheckBox = view.findViewById(R.id.crime_solved)
        suspectButton = view.findViewById(R.id.crime_suspect)
        reportButton = view.findViewById(R.id.crime_report)
        callButton = view.findViewById(R.id.crime_call)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            suspectPhone = it.getString(KEY_PHONE, "")
            contactId = it.getLong(KEY_CONTACT_ID, -1L)
        }

        titleField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (::crime.isInitialized) crime.title = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        solvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (::crime.isInitialized) crime.isSolved = isChecked
        }

        val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        CrimeLab.get(requireActivity()).getCrime(crimeId)
            .observe(viewLifecycleOwner) { c ->
                if (c != null) {
                    crime = c
                    updateUI()
                    applyPendingSuspect()
                }
            }
    }

    override fun onStart() {
        super.onStart()

        // Неявный интент №1: отправка отчёта (ACTION_SEND + список выбора)
        reportButton.setOnClickListener {
            if (!::crime.isInitialized) return@setOnClickListener
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                startActivity(Intent.createChooser(intent, getString(R.string.send_report)))
            }
        }

        // Неявный интент №2: выбор подозреваемого из адресной книги (ACTION_PICK)
        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }
            // п.8 методички: если приложения контактов нет — блокируем кнопку
            val resolvedActivity = requireActivity().packageManager
                .resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY)
            isEnabled = resolvedActivity != null
        }

        // Задание 4.2: неявный интент №3 — ACTION_DIAL (безопаснее ACTION_CALL)
        callButton.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$suspectPhone"))
            if (suspectPhone.isNotBlank() &&
                dialIntent.resolveActivity(requireActivity().packageManager) != null
            ) {
                startActivity(dialIntent)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::crime.isInitialized) {
            CrimeLab.get(requireActivity()).updateCrime(crime)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PHONE, suspectPhone)
        outState.putLong(KEY_CONTACT_ID, contactId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri = data.data ?: return
                val queryFields = arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts._ID
                )
                requireActivity().contentResolver
                    .query(contactUri, queryFields, null, null, null)?.use { cursor ->
                        if (cursor.count == 0) return
                        cursor.moveToFirst()
                        pendingSuspect = cursor.getString(0)
                        contactId = cursor.getLong(1)
                    }
                applyPendingSuspect()
                loadPhoneWithPermission()
            }
        }
    }

    // Сохраняем сразу: onActivityResult может прийти раньше, чем загрузится crime из БД
    private fun applyPendingSuspect() {
        val suspect = pendingSuspect ?: return
        if (!::crime.isInitialized) return
        crime.suspect = suspect
        pendingSuspect = null
        CrimeLab.get(requireActivity()).updateCrime(crime)
        if (view != null) updateUI()
    }

    private fun loadPhoneWithPermission() {
        if (contactId < 0) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            loadPhoneNumber()
        } else {
            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun loadPhoneNumber() {
        if (contactId < 0) return
        requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                suspectPhone = cursor.getString(0)
            }
        }
        if (view != null) updateCallButton()
    }

    private fun updateUI() {
        // Не перезаписываем поле, если текст совпадает — иначе прыгает курсор при вводе
        if (titleField.text.toString() != crime.title) {
            titleField.setText(crime.title)
        }
        dateButton.text = android.text.format.DateFormat.format(DATE_FORMAT, crime.date)
        if (solvedCheckBox.isChecked != crime.isSolved) {
            solvedCheckBox.isChecked = crime.isSolved
        }
        suspectButton.text =
            if (crime.suspect.isNotBlank()) crime.suspect
            else getString(R.string.crime_suspect_text)
        updateCallButton()
    }

    private fun updateCallButton() {
        callButton.isEnabled = suspectPhone.isNotBlank()
    }

    private fun getCrimeReport(): String {
        val solvedString = getString(
            if (crime.isSolved) R.string.crime_report_solved else R.string.crime_report_unsolved
        )
        val dateString =
            android.text.format.DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect =
            if (crime.suspect.isBlank()) getString(R.string.crime_report_no_suspect)
            else getString(R.string.crime_report_suspect, crime.suspect)
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply { putSerializable(ARG_CRIME_ID, crimeId) }
            return CrimeFragment().apply { arguments = args }
        }
    }
}