package com.febrivio.sumbercomplang.fragment

import android.app.AlertDialog
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
import com.febrivio.sumbercomplang.model.ProfileUpdateRequest
import com.febrivio.sumbercomplang.model.ProfileUpdateResponse
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        b.tvUsername.text = session.getUserUsername()
        b.tvPhone.text = session.getUserNoHp()

        setupClickListeners()

        return v
    }

    private fun setupClickListeners() {
        // Logout button
        b.btnLogout.setOnClickListener {

        }

        b.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin logout?")
                .setPositiveButton("Logout") { dialog, _ ->
                    session.logout()
                    dialog.dismiss()
                    val intent = Intent(thisParent, LoginActivity::class.java)
                    startActivity(intent)
                    thisParent.finish()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
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
            b.tvUsername.visibility = View.GONE
            b.etusername.visibility = View.VISIBLE
            b.tvPhone.visibility = View.GONE
            b.etPhone.visibility = View.VISIBLE


            // Populate edit fields with current values
            b.etName.setText(b.tvName.text)
            b.etusername.setText(b.tvUsername.text)
            b.etPhone.setText(b.tvPhone.text)

            // Show save/cancel buttons
            b.layoutSaveButtons.visibility = View.VISIBLE
        } else {
            // Switch back to view mode
            b.tvName.visibility = View.VISIBLE
            b.etName.visibility = View.GONE
            b.tvUsername.visibility = View.VISIBLE
            b.etusername.visibility = View.GONE
            b.tvPhone.visibility = View.VISIBLE
            b.etPhone.visibility = View.GONE

            // Hide save/cancel buttons
            b.layoutSaveButtons.visibility = View.GONE
        }
    }

    private fun saveChanges() {
        // Get the updated values
        val newName = b.etName.text.toString().trim()
        val newUsername = b.etusername.text.toString().trim()
        val newPhone = b.etPhone.text.toString().trim()

        // Validate inputs
        if (newName.isEmpty() || newUsername.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(thisParent, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        b.progressBar.visibility = View.VISIBLE
        b.btnSaveChanges.isEnabled = false
        b.btnCancel.isEnabled = false

        // Create the request
        val profileUpdateRequest = ProfileUpdateRequest(newName, newUsername, newPhone)

        // Mendapatkan token dari SessionManager
        val token = SessionManager(thisParent).getToken()

        // Make the API call
        val apiService = ApiServiceAuth(thisParent, token)

        apiService.updateProfile(profileUpdateRequest).enqueue(object : Callback<ProfileUpdateResponse> {
            override fun onResponse(call: Call<ProfileUpdateResponse>, response: Response<ProfileUpdateResponse>) {
                // Hide loading indicator
                b.progressBar.visibility = View.GONE
                b.btnSaveChanges.isEnabled = true
                b.btnCancel.isEnabled = true

                if (response.isSuccessful) {
                    val res = response.body()
                    when (res?.code) {
                        200, 201 -> {
                            // Update was successful

                            // Update session data
                            session.saveUserName(newName)
                            session.saveUserUsername(newUsername)
                            session.saveUserNoHp(newPhone)

                            // Update UI
                            b.tvName.text = newName
                            b.tvUsername.text = newUsername
                            b.tvPhone.text = newPhone

                            // Exit edit mode
                            toggleEditMode()

                            // Show success message
                            Toast.makeText(thisParent, res.message ?: "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                        422 -> {
                            // Validation errors
                            val errors = res.errors
                            val errorMessage = StringBuilder()
                            errors?.forEach { (field, messages) ->
                                errorMessage.append("${messages.joinToString(", ")}\n")
                            }
                            Toast.makeText(thisParent, errorMessage.toString(), Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            // Other error codes
                            Toast.makeText(thisParent, res?.message ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Response not successful (network error, server error, etc)
                    Toast.makeText(thisParent, "Gagal memperbarui profil: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProfileUpdateResponse>, t: Throwable) {
                // Hide loading indicator
                b.progressBar.visibility = View.GONE
                b.btnSaveChanges.isEnabled = true
                b.btnCancel.isEnabled = true

                // Show error message
                Toast.makeText(thisParent, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cancelEdit() {
        // Just switch back to view mode without saving changes
        isEditMode = false
        b.tvName.visibility = View.VISIBLE
        b.etName.visibility = View.GONE
        b.tvUsername.visibility = View.VISIBLE
        b.etusername.visibility = View.GONE
        b.tvPhone.visibility = View.VISIBLE
        b.etPhone.visibility = View.GONE
        b.layoutSaveButtons.visibility = View.GONE
    }
}