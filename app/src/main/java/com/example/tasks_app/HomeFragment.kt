package com.example.tasks_app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.tasks_app.databinding.FragmentHomeBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ChecklistItem(
    val text: String = "",
    val checked: Boolean = false
)

data class Task(
    val name: String = "",
    val checklist: List<ChecklistItem> = listOf()
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTasks()

        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_task)
        fab.setOnClickListener {
            showTaskDialog(null, null)
        }
    }

    private fun showTaskDialog(taskId: String?, existingTask: Task?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val editTaskName = dialogView.findViewById<EditText>(R.id.editTaskName)
        val checklistContainer = dialogView.findViewById<LinearLayout>(R.id.checklistContainer)
        val buttonAddItem = dialogView.findViewById<Button>(R.id.buttonAddItem)

        // Vorhandene Taskdaten setzen
        existingTask?.let {
            editTaskName.setText(it.name)
            it.checklist.forEach { item ->
                val itemLayout = createChecklistItemView(item.text, item.checked)
                checklistContainer.addView(itemLayout)
            }
        }

        // Neues Item hinzufügen
        buttonAddItem.setOnClickListener {
            val itemLayout = createChecklistItemView("", false)
            checklistContainer.addView(itemLayout)
        }

        // Dialog anzeigen
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (taskId != null) "Task bearbeiten" else "Neuer Task")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val name = editTaskName.text.toString()
                val checklist = mutableListOf<ChecklistItem>()

                for (i in 0 until checklistContainer.childCount) {
                    val itemLayout = checklistContainer.getChildAt(i) as LinearLayout
                    val checkBox = itemLayout.getChildAt(0) as CheckBox
                    val itemText = itemLayout.getChildAt(1) as CheckBox

                    checklist.add(ChecklistItem(itemText.text.toString(), checkBox.isChecked))
                }

                saveTaskToFirestore(taskId, Task(name, checklist))
            }
            .setNegativeButton("Abbrechen", null)

        // Nur beim Bearbeiten → Löschbutton zeigen
        if (taskId != null) {
            builder.setNeutralButton("Löschen") { _, _ ->
                deleteTaskFromFirestore(taskId)
            }
        }

        builder.show()

    }

    private fun createChecklistItemView(text: String, checked: Boolean): LinearLayout {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(10, 80, 0, 0)
            }
        }

        val checkBox = CheckBox(requireContext()).apply {
            isChecked = checked
        }

        val editText = EditText(requireContext()).apply {
            setText(text)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "Item"
        }

        itemLayout.addView(checkBox)
        itemLayout.addView(editText)

        return itemLayout
    }

    private fun saveTaskToFirestore(taskId: String?, task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val taskRef = if (taskId != null) {
            db.collection("users").document(userId).collection("tasks").document(taskId)
        } else {
            db.collection("users").document(userId).collection("tasks").document()
        }

        taskRef.set(task)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Task gespeichert ✅", Toast.LENGTH_SHORT).show()
                binding.taskContainer.removeAllViews()
                loadTasks()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTasks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val taskId = document.id
                    val name = document.getString("name") ?: ""
                    val checklistRaw = document["checklist"] as? List<Map<String, Any>> ?: emptyList()

                    val checklist = checklistRaw.map {
                        ChecklistItem(
                            text = it["text"] as? String ?: "",
                            checked = it["checked"] as? Boolean ?: false
                        )
                    }

                    addTaskToUI(taskId, Task(name, checklist))
                }
            }
    }

    private fun addTaskToUI(taskId: String, task: Task) {
        val cardLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24) // Innenabstand
            setBackgroundResource(R.drawable.task_card_background)

            // Klick zum Bearbeiten
            setOnClickListener {
                showTaskDialog(taskId, task)
            }

            // Außenabstand
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(64, 16, 64, 16) //
            }
            layoutParams = params
        }

        val title = TextView(requireContext()).apply {
            text = task.name
            textSize = 20f
            setTextColor(Color.WHITE)
        }

        cardLayout.addView(title)
        binding.taskContainer.addView(cardLayout)
    }



    private fun deleteTaskFromFirestore(taskId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Task gelöscht 🗑️", Toast.LENGTH_SHORT).show()
                binding.taskContainer.removeAllViews()
                loadTasks()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Fehler beim Löschen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
