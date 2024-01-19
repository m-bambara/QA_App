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
        }

        if (question.favorite == "true") {
            //画像表示
            binding.favoriteImageView.setImageResource(R.drawable.ic_star)
        } else {
            //画像表示
            binding.favoriteImageView.setImageResource(R.drawable.ic_star_border)
        }

        binding.favoriteImageView.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val questionUid = question.questionUid
                val userUid = user.uid


                // "favorites"ノードに質問のUIDをキーとして、お気に入り状態を保存
                val favoriteRef = databaseReference.child("favorites").child(userUid).child(questionUid)


                favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val isFavorite = dataSnapshot.getValue(String::class.java)
                        val newFavoriteStatus = if (isFavorite == null || isFavorite == "false") "true" else "false"

                        favoriteRef.setValue(newFavoriteStatus)
                        val imageResource = if (newFavoriteStatus == "true") R.drawable.ic_star else R.drawable.ic_star_border
                        binding.favoriteImageView.setImageResource(imageResource)

                        // お気に入り状態が更新されたことをローカルのquestionオブジェクトに反映
                        question.favorite = newFavoriteStatus
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w("QuestionDetailActivity", "Failed to read favorite", databaseError.toException())
                    }
                })
           }

            val dataBaseReference = FirebaseDatabase.getInstance().reference
            answerRef = dataBaseReference.child(ContentsPATH).child(question.genre.toString())
                .child(question.questionUid).child(AnswersPATH)
            answerRef.addChildEventListener(eventListener)
        }
    }
}
