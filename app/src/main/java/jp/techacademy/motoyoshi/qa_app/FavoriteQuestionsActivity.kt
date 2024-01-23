package jp.techacademy.motoyoshi.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.motoyoshi.qa_app.databinding.ActivityFavoriteQuestionsBinding


class FavoriteQuestionsActivity : AppCompatActivity() , NavigationView.OnNavigationItemSelectedListener{
//class FavoriteQuestionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteQuestionsBinding
    private lateinit var databaseReference: DatabaseReference
    private var questionArrayList = ArrayList<Question>()
    private lateinit var adapter: QuestionsListAdapter

    private var genre = 0
    private var genreRef: DatabaseReference? = null

    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>
            val title = map["title"] as? String ?: ""
            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
//            val favorite = map["favorite"] as? String ?: "" //favorite追加 booleanではなく、Stringの書き替えで処理 => favoritesにqUidがあるかで判断するため、不要
            val uid = map["uid"] as? String ?: ""
            val imageString = map["image"] as? String ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<*, *>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val map1 = answerMap[key] as Map<*, *>
                    val map1Body = map1["body"] as? String ?: ""
                    val map1Name = map1["name"] as? String ?: ""
                    val map1Uid = map1["uid"] as? String ?: ""
                    val map1AnswerUid = key as? String ?: ""
                    val answer = Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                    answerArrayList.add(answer)
                }
            }

//            val question = Question(
//                title, body, name, uid, dataSnapshot.key ?: "",
//                genre, favorite, bytes, answerArrayList
//            )
            val question = Question(
                title, body, name, uid, dataSnapshot.key ?: "",
                genre, bytes, answerArrayList
            )
            questionArrayList.add(question)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            // 変更があったQuestionを探す
            for (question in questionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<*, *>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val map1 = answerMap[key] as Map<*, *>
                            val map1Body = map1["body"] as? String ?: ""
                            val map1Name = map1["name"] as? String ?: ""
                            val map1Uid = map1["uid"] as? String ?: ""
                            val map1AnswerUid = key as? String ?: ""
                            val answer = Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                            question.answers.add(answer)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {}
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
        override fun onCancelled(p0: DatabaseError) {}
    }

    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        // 1:趣味を既定の選択とする
        if(genre == 0) {
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ナビゲーションビューのリスナー設定
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        // ツールバーの設定
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.content.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Firebaseの参照初期化
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = QuestionsListAdapter(this)
        questionArrayList = ArrayList()
        adapter.notifyDataSetChanged()

        // ログイン状態の確認
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // ログインしていない場合、ログイン画面に遷移
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // ログインしている場合、お気に入り質問を取得
            loadFavoriteQuestions(currentUser.uid)
        }

        binding.content.inner.listView.setOnItemClickListener { _, _, position, _ ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", questionArrayList[position])
            startActivity(intent)
        }
    }

    private fun loadFavoriteQuestions(userUid: String) {
        val favoriteRef = databaseReference.child("favorites").child(userUid)
        favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                questionArrayList.clear()
                dataSnapshot.children.forEach { favoriteSnapshot ->
                    val questionUid = favoriteSnapshot.key
                    val genre = favoriteSnapshot.value.toString()
                    val questionRef = databaseReference.child("contents").child(genre).child(
                        questionUid.toString()
                    )
                    questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(questionSnapshot: DataSnapshot) {
                            val question = questionSnapshot.getValue(Question::class.java)
                            if (question != null) {
                                questionArrayList.add(question)
                                adapter.setQuestionArrayList(questionArrayList)
                                binding.content.inner.listView.adapter = adapter
                            }
                        }


                        override fun onCancelled(databaseError: DatabaseError) {
                            // エラー処理
                        }
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // エラー処理
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.nav_hobby -> {
                binding.content.toolbar.title = getString(R.string.menu_hobby_label)
                genre = 1
            }
            R.id.nav_life -> {
                binding.content.toolbar.title = getString(R.string.menu_life_label)
                genre = 2
            }
            R.id.nav_health -> {
                binding.content.toolbar.title = getString(R.string.menu_health_label)
                genre = 3
            }
            R.id.nav_computer -> {
                binding.content.toolbar.title = getString(R.string.menu_computer_label)
                genre = 4
            }
            R.id.nav_fav -> {
                // お気に入り画面への遷移
                val intent = Intent(this, FavoriteQuestionsActivity::class.java)
                startActivity(intent)
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        questionArrayList.clear()
        adapter.setQuestionArrayList(questionArrayList)
        binding.content.inner.listView.adapter = adapter

        // 選択したジャンルにリスナーを登録する
        if (genreRef != null) {
            genreRef!!.removeEventListener(eventListener)
        }
        genreRef = databaseReference.child(ContentsPATH).child(genre.toString())
        genreRef!!.addChildEventListener(eventListener)

        return true
    }
}
