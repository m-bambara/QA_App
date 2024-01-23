package jp.techacademy.motoyoshi.qa_app

import java.io.Serializable
import java.util.ArrayList

//class Question(
//    val title: String,
//    val body: String,
//    val name: String,
//    val uid: String,
//    val questionUid: String,
//    val genre: Int,
//    bytes: ByteArray,
//    val answers: ArrayList<Answer>
//) : Serializable {
//    val imageBytes: ByteArray
//
//    init {
//        imageBytes = bytes.clone()
//    }
//}

//★コンストラクタを要求された理由が分からない
class Question() : Serializable {
    var title: String = ""
    var body: String = ""
    var name: String = ""
    var uid: String = ""
    var questionUid: String = ""
    var genre: Int = 0
    var imageBytes: ByteArray = byteArrayOf()
    var answers: ArrayList<Answer> = arrayListOf()

    constructor(
        title: String,
        body: String,
        name: String,
        uid: String,
        questionUid: String,
        genre: Int,
        bytes: ByteArray,
        answers: ArrayList<Answer>
    ) : this() {
        this.title = title
        this.body = body
        this.name = name
        this.uid = uid
        this.questionUid = questionUid
        this.genre = genre
        this.imageBytes = bytes.clone()
        this.answers = answers
    }
}