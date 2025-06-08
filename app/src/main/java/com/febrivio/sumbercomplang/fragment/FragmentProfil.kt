package com.febrivio.sumbercomplang.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.febrivio.sumbercomplang.LoginActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.databinding.FragmentProfilBinding
import com.febrivio.sumbercomplang.services.SessionManager

class FragmentProfil : Fragment() {

    lateinit var b: FragmentProfilBinding
    private lateinit var thisParent: MainActivity
    private lateinit var v: View
    private lateinit var session: SessionManager
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inisialisasi parent activity dan view binding
        thisParent = activity as MainActivity
        b = FragmentProfilBinding.inflate(inflater, container, false)
        v = b.root

        // Inisialisasi session
        session = SessionManager(thisParent)

        // Set nama dan email pengguna dari session
        b.tvName.text = session.getUserName()
        b.tvEmail.text = session.getUserEmail()

        // Set tanggal bergabung (example - in a real app, this would come from user data)
        b.tvJoined.text = "15 Mei 2025"

        setupClickListeners()

        return v
    }

    private fun setupClickListeners() {
        // Logout button
        b.btnLogout.setOnClickListener {
            session.logout()
            val intent = Intent(thisParent, LoginActivity::class.java)
            startActivity(intent)
            thisParent.finish()
        }

        // Edit Profile button
        b.btnChangePassword.setOnClickListener {
            toggleEditMode()
        }

        // Save Changes button
        b.btnSaveChanges.setOnClickListener {
            saveChanges()
        }

        // Cancel button
        b.btnCancel.setOnClickListener {
            cancelEdit()
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        if (isEditMode) {
            // Switch to edit mode
            b.tvName.visibility = View.GONE
            b.etName.visibility = View.VISIBLE
            b.tvEmail.visibility = View.GONE
            b.etEmail.visibility = View.VISIBLE

            // Populate edit fields with current values
            b.etName.setText(b.tvName.text)
            b.etEmail.setText(b.tvEmail.text)

            // Show save/cancel buttons
            b.layoutSaveButtons.visibility = View.VISIBLE
        } else {
            // Switch back to view mode
            b.tvName.visibility = View.VISIBLE
            b.etName.visibility = View.GONE
            b.tvEmail.visibility = View.VISIBLE
            b.etEmail.visibility = View.GONE

            // Hide save/cancel buttons
            b.layoutSaveButtons.visibility = View.GONE
        }
    }

    private fun saveChanges() {
        // Get the updated values
        val newName = b.etName.text.toString().trim()
        val newEmail = b.etEmail.text.toString().trim()

        // Validate inputs
        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(thisParent, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Simple email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(thisParent, "Format email tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Update session data
        session.saveUserName(newName)
        session.saveUserEmail(newEmail)

        // Update UI
        b.tvName.text = newName
        b.tvEmail.text = newEmail

        // Exit edit mode
        toggleEditMode()

        // Show success message
        Toast.makeText(thisParent, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
    }

    private fun cancelEdit() {
        // Just switch back to view mode without saving changes
        isEditMode = false
        b.tvName.visibility = View.VISIBLE
        b.etName.visibility = View.GONE
        b.tvEmail.visibility = View.VISIBLE
        b.etEmail.visibility = View.GONE
        b.layoutSaveButtons.visibility = View.GONE
    }
}