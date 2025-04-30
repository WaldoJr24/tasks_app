package com.example.tasks_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.example.tasks_app.databinding.FragmentAuthBinding
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.findNavController



class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private lateinit var auth: FirebaseAuth

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button: Button = view.findViewById(R.id.button_first)
        auth = FirebaseAuth.getInstance()



        button.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString().trim()
            val password = binding.editTextTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Bitte E-Mail und Passwort eingeben", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Login erfolgreich
                        val user = auth.currentUser
                        Toast.makeText(requireContext(), "Willkommen ${user?.email}", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_AuthFragment_to_HomeFragment)

                    } else {
                        // Fehler
                        Toast.makeText(requireContext(), "Login fehlgeschlagen: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString().trim()
            val password = binding.editTextTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Bitte E-Mail und Passwort eingeben", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Toast.makeText(requireContext(), "Registrierung erfolgreich: ${user?.email}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Fehler bei der Registrierung: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}