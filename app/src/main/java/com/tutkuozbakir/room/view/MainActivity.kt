package com.tutkuozbakir.room.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.tutkuozbakir.room.R
import com.tutkuozbakir.room.adapter.PlaceAdapter
import com.tutkuozbakir.room.databinding.ActivityMainBinding
import com.tutkuozbakir.room.model.Place
import com.tutkuozbakir.room.roomDb.PlaceDao
import com.tutkuozbakir.room.roomDb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    //private var placeList = ArrayList<Place>()
    private lateinit var adapter: PlaceAdapter
    private lateinit var database: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java,"Places")
            .build()
        placeDao = database.placeDao()


        compositeDisposable.add(
            placeDao.getAllData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this@MainActivity::handleResponse)
        )

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            //drag direction in recyclerview
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            // direction we want to be able to swipe the items
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position =viewHolder.adapterPosition
                val item = adapter.placeList[position]

                compositeDisposable.add(
                    placeDao.delete(item)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe())

                Snackbar.make(view, "Successfully deleted item ", Snackbar.LENGTH_LONG).apply {
                    setAction("Undo") {
                        //save article again that we just deleted
                        compositeDisposable.add(
                            placeDao.insert(item)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe())

                    }.show()
            }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).apply {
            // attach itemTouchHelperCallback to recyclerview
            attachToRecyclerView(binding.recyclerView)
        }

    }

    private fun handleResponse(placeList : List<Place>){
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PlaceAdapter(placeList)
        binding.recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater.inflate(R.menu.options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = Intent(this@MainActivity, SecondActivity::class.java)
        startActivity(intent)
        return super.onOptionsItemSelected(item)
    }
}