package jp.techacademy.motoyoshi.qa_app

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.motoyoshi.qa_app.databinding.ActivityQuestionDetailBinding

class QuestionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionDetailBinding
    private lateinit var databaseReference: DatabaseReference

    private lateinit var question: Question
    private lateinit var adapter: QuestionDetailListAdapter
    private lateinit var answerRef: DatabaseReference

    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in question.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            question.answers.add(answer)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Database の参照を初期化
        databaseReference = FirebaseDatabase.getInstance().reference

        // 渡ってきたQuestionのオブジェクトを保持する
        // API33以上でgetSerializableExtra(key)が非推奨となったため処理を分岐
        @Suppress("UNCHECKED_CAST", "DEPRECATION", "DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL")
        question = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra("question", Question::class.java)!!
        else
            intent.getSerializableExtra("question") as? Question!!

        title = question.title

        // ListViewの準備
        adapter = QuestionDetailListAdapter(this, question)
        binding.listView.adapter = adapter
        adapter.notifyDataSetChanged()

        binding.fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", question)
                startActivity(intent)
                // --- ここまで ---
            }
        }

        //ログインしているか
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        // ログインしていなければ★を非表示にする
        if (user == null) {
            //星を非表示にする
            binding.favoriteImageView.visibility = View.INVISIBLE
        } else {
            binding.favoriteImageView.visibility = View.VISIBLE

            val favoriteRef = databaseReference.child("favorites").child(user.uid).child(question.questionUid)
            // 現在のお気に入り状態をチェックし、星の画像を設定
            setFavoriteImage(favoriteRef)

            // 星の画像をクリックしたときの処理
            binding.favoriteImageView.setOnClickListener {
                favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // 既にお気に入りなら削除
                            favoriteRef.removeValue()
                        } else {
                            // お気に入りでなければ追加
                            favoriteRef.setValue(question.genre.toString())
                        }
                        setFavoriteImage(favoriteRef)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w("QuestionDetailActivity", "Failed to read favorite", databaseError.toException())
                    }
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()

    }

    private fun setFavoriteImage(favoriteRef: DatabaseReference) {
        favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    binding.favoriteImageView.setImageResource(R.drawable.ic_star)
                } else {
                    binding.favoriteImageView.setImageResource(R.drawable.ic_star_border)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("QuestionDetailActivity", "Failed to read favorite", databaseError.toException())
            }
        })
    }

}
