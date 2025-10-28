package com.example.levelup_gamerpractica.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.levelup_gamerpractica.data.local.dao.CartDao
import com.example.levelup_gamerpractica.data.local.dao.ProductDao
import com.example.levelup_gamerpractica.data.local.dao.UserDao
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import com.example.levelup_gamerpractica.data.local.entities.Product
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Product::class, CartItem::class],
    version = 1, // Incrementar si cambias el esquema
    exportSchema = false // Puedes ponerlo a true si quieres exportar el esquema
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "levelup_gamer_database" // Nombre del archivo de la BD
                )
                    // .fallbackToDestructiveMigration() // Usar con cuidado en desarrollo
                    .addCallback(DatabaseCallback(context)) // Para pre-popular datos
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // Callback para pre-popular la base de datos con productos la primera vez
    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.productDao())
                }
            }
        }

        suspend fun populateDatabase(productDao: ProductDao) {
            // Aquí puedes insertar tu lista inicial de productos
            val initialProducts = listOf(
                Product(
                    id = 1, name = "Catan", price = "$29.990 CLP", category = "Juegos de Mesa",
                    image = "https://devirinvestments.s3.eu-west-1.amazonaws.com/img/catalog/product/8436017220100-1200-face3d.jpg",
                    description = ": Un clásico juego de estrategia...",
                    manufacturer = "Devir", distributor = "Asmodee"),
                Product(
                    id = 2, name = "Carcassonne", price = "$24.990 CLP", category = "Juegos de Mesa",
                    image = "https://devirinvestments.s3.eu-west-1.amazonaws.com/img/catalog/product/8436017222593-1200-frontflat-copy.jpg",
                    description = "Un juego de colocación de fichas...",
                    manufacturer = "Hans im Glück", distributor = "Devir"),
                Product(
                    id = 3, name = "Controlador Inalámbrico Xbox", price = "$59.990 CLP", category = "Accesorios",
                    image = "https://prophonechile.cl/wp-content/uploads/2023/11/purpleeee.png",
                    description = "Experimenta el diseño modernizado del control...",
                    manufacturer = "Microsoft", distributor = "Microsoft Chile"
                ),
                Product(
                    id = 4, name = "Auriculares Gamer HyperX Cloud II", price = "$79.990 CLP", category = "Accesorios",
                    image = "https://row.hyperx.com/cdn/shop/files/hyperx_cloud_ii_red_1_main.jpg?v=1737720332",
                    description = "Un auricular legendario por su comodidad...",
                    manufacturer = "HyperX (HP Inc.)", distributor = "HP Chile"
                ),
                Product(
                    id = 5, name = "PlayStation 5", price = "$549.990 CLP", category = "Consolas",
                    image = "https://static.pcfactory.cl/imagenes/53428-3.jpg",
                    description = "La consola PS5 desata nuevas posibilidades...",
                    manufacturer = "Sony Interactive Entertainment", distributor = "Sony Chile"
                ),
                Product(
                    id = 6, name = "PC Gamer ASUS ROG Strix", price = "$1.299.990 CLP", category = "Computadores Gamers",
                    image = "https://www.asus.com/media/Odin/Websites/global/Series/52.png",
                    description = "Diseñado para el rendimiento extremo...",
                    manufacturer = "ASUS (Republic of Gamers)", distributor = "ASUS Chile"
                ),
                Product(
                    id = 7, name = "Silla Gamer Secretlab Titan", price = "$349.990 CLP", category = "Sillas Gamers",
                    image = "https://images.secretlab.co/turntable/tr:n-w_750/R22PU-Stealth_02.jpg",
                    description = "Diseñada para una comodidad ergonómica absoluta...",
                    manufacturer = "Secretlab", distributor = "Secretlab Chile / Distribuidores Autorizados"
                ),
                Product(
                    id = 8, name = "Mouse Gamer Logitech G502 HERO", price = "$49.990 CLP", category = "Mouse",
                    image = "https://media.spdigital.cl/thumbnails/products/snbujg5__29f7dd61_thumbnail_4096.jpg",
                    description = "Uno de los mouse más populares del mundo...",
                    manufacturer = "Logitech G", distributor = "Logitech Chile / Distribuidores Autorizados"
                ),
                Product(
                    id = 9, name = "Mousepad Razer Goliathus Chroma", price = "$29.990 CLP", category = "Mousepad",
                    image = "https://cl-cenco-pim-resizer.ecomm.cencosud.com/unsafe/adaptive-fit-in/3840x0/filters:quality(75)/prd-cl/product-medias/bb228f59-3aa1-4d9e-bbf4-b5f71bc89ca0/MK8YWFQA7I/MK8YWFQA7I-1/1737041557738-MK8YWFQA7I-1-1.jpg",
                    description = "Ilumina tu setup con el Razer Goliathus Chroma...",
                    manufacturer = "Razer", distributor = "Razer Latin America"
                ),
                Product(
                    id = 10, name = "Polera Gamer Personalizada \"Level-Up\"", price = "$14.990 CLP", category = "Poleras Personalizadas",
                    image = "polera",
                    description = "¡Viste tu pasión! Crea tu propia polera gamer...",
                    manufacturer = "LevelUp-Gamer (Producción Local)", distributor = "LevelUp-Gamer"
                )
            )
            productDao.insertAll(initialProducts)
        }
    }
}