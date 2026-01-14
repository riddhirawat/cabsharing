const geocode = require("./geocode");      // address → lat/lon
const getDistanceKm = require("./distance"); // distance logic
// ===========================================================
// 🛠️ GoCab Backend - index.js
// ===========================================================

const express = require("express");
const sql = require("mssql");
const cors = require("cors");
require("dotenv").config();

const app = express();

// ===========================================================
// 🧩 Middlewares
// ===========================================================
app.use(cors());

// ✅ Safer JSON body parser (prevents crash on empty body)
app.use(express.json({
  verify: (req, res, buf, encoding) => {
    if (buf && buf.length === 0) {
      console.warn("⚠️ Received empty JSON body for:", req.originalUrl);
    }
  }
}));

// ===========================================================
// 🔧 Azure SQL Configuration
// ===========================================================
const dbConfig = {
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  server: process.env.DB_SERVER,
  database: process.env.DB_NAME,
  port: parseInt(process.env.DB_PORT) || 1433,
  options: {
    encrypt: true, // Required for Azure
    trustServerCertificate: false,
  },
};

console.log("ℹ️ DB Config:", {
  server: process.env.DB_SERVER,
  user: process.env.DB_USER,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT,
});

// ===========================================================
// 🧩 Connect to Database Once
// ===========================================================
sql.connect(dbConfig)
  .then(pool => {
    if (pool.connected) {
      console.log("✅ Connected to Azure SQL Database");
      // Simple test query
      pool.request().query("SELECT 1 AS number")
        .then(result => console.log("✅ Test query succeeded:", result.recordset))
        .catch(err => console.error("❌ Test query failed:", err.message));
    }
  })
  .catch(err => {
    console.error("❌ Database connection failed:");
    console.error("Server:", process.env.DB_SERVER);
    console.error("User:", process.env.DB_USER);
    console.error("DB:", process.env.DB_NAME);
    console.error("Error Details:", err.message);
  });

// ===========================================================
// 🌐 Root Route
// ===========================================================
app.get("/", (req, res) => {
  res.send("GoCab Backend is running 🚀");
});
// ✅ Temporary route to check if /api/user/register exists
app.get("/api/user/register", (req, res) => {
  res.send("✅ POST /api/user/register route exists!");
});

// ===========================================================
// 👩‍🎓 Fetch All Students
// ===========================================================
app.get("/students", async (req, res) => {
  try {
    const pool = await sql.connect(dbConfig);
    const result = await pool.request().query("SELECT * FROM Student");
    res.json(result.recordset);
  } catch (err) {
    console.error("❌ Error fetching students:", err.message);
    res.status(500).send("Error fetching students: " + err.message);
  }
});

// ===========================================================
// 👤 Register a New User (Step 1)
// ===========================================================
app.post("/api/user/register", async (req, res) => {
  const { firebase_uid, email_id, user_type } = req.body;

  if (!firebase_uid || !email_id || !user_type) {
    console.warn("⚠️ Missing required fields in /api/user/register:", req.body);
    return res.status(400).json({ error: "Missing required fields" });
  }
  console.log("📩 Received user registration:", req.body);

  try {
    const pool = await sql.connect(dbConfig);

    // ✅ Insert only into User table
    await pool.request()
      .input("firebase_uid", sql.VarChar, firebase_uid)
      .input("email_id", sql.VarChar, email_id)
      .input("user_type", sql.VarChar, user_type)
      .query(`
        INSERT INTO [User] (firebase_uid, email_id, user_type)
        VALUES (@firebase_uid, @email_id, @user_type)
      `);

    console.log(`✅ User inserted: ${email_id} (${user_type})`);

    // ⚠️ Do NOT auto-create student record now.
    // The student record will be created later in /api/student/add
    // after the user fills the personal information form.

    res.status(201).json({
      success: true,
      message: "User registered successfully. Complete personal info next."
    });

  } catch (err) {
    console.error("❌ Error inserting user:", err.message);
    res.status(500).json({ success: false, error: err.message });
  }
});

// ===========================================================
// 🎓 Add Student + Guardian Information Together
// ===========================================================
app.post("/api/student/add", async (req, res) => {
  const {
    firebase_uid, S_email_id, S_name, Smartcard_id,
    College_name, dateofbirth, gender, aadhar_number,
    course, branch, year, Permanent_address, hostel,
    G_name, G_phone_no, G_eid
  } = req.body;

  // ✅ Validation for student fields
  if (!firebase_uid || !S_email_id || !S_name) {
    console.warn("⚠️ Missing required student fields:", req.body);
    return res.status(400).json({ success: false, message: "Missing required student details." });
  }

  // ✅ Validation for guardian fields
  if (!G_name || !G_phone_no || !G_eid) {
    console.warn("⚠️ Missing required guardian fields:", req.body);
    return res.status(400).json({ success: false, message: "Missing required guardian details." });
  }

  console.log("📩 Received full student + guardian info:", req.body);

  try {
    const pool = await sql.connect(dbConfig);

    // 🔹 Begin a transaction so both inserts happen together
    const transaction = new sql.Transaction(pool);
    await transaction.begin();

    try {
      // 🔸 Insert Student Info
      await transaction.request()
        .input("firebase_uid", sql.VarChar, firebase_uid)
        .input("S_email_id", sql.VarChar, S_email_id)
        .input("S_name", sql.VarChar, S_name)
        .input("Smartcard_id", sql.VarChar, Smartcard_id)
        .input("College_name", sql.VarChar, College_name)
        .input("dateofbirth", sql.Date, dateofbirth)
        .input("gender", sql.VarChar, gender)
        .input("aadhar_number", sql.VarChar, aadhar_number)
        .input("course", sql.VarChar, course)
        .input("branch", sql.VarChar, branch)
        .input("year", sql.VarChar, year)
        .input("Permanent_address", sql.VarChar, Permanent_address)
        .input("hostel", sql.VarChar, hostel)
        .query(`
          INSERT INTO Student (
            firebase_uid, S_email_id, S_name, Smartcard_id,
            College_name, dateofbirth, gender, aadhar_number,
            course, branch, year, Permanent_address, hostel
          )
          VALUES (
            @firebase_uid, @S_email_id, @S_name, @Smartcard_id,
            @College_name, @dateofbirth, @gender, @aadhar_number,
            @course, @branch, @year, @Permanent_address, @hostel
          )
        `);

      // 🔸 Insert Guardian Info
      await transaction.request()
        .input("S_email_id", sql.VarChar, S_email_id)
        .input("G_name", sql.VarChar, G_name)
        .input("G_phone_no", sql.VarChar, G_phone_no)
        .input("G_eid", sql.VarChar, G_eid)
        .query(`
          INSERT INTO Parents_Information (S_email_id, G_name, G_phone_no, G_eid)
          VALUES (@S_email_id, @G_name, @G_phone_no, @G_eid)
        `);

      // ✅ Commit transaction
      await transaction.commit();

      console.log(`✅ Student + Guardian info added for: ${S_email_id}`);
      res.status(200).json({
        success: true,
        message: "Student and guardian information saved successfully."
      });

    } catch (err) {
      // ❌ Rollback if anything fails
      await transaction.rollback();
      console.error("❌ Transaction failed:", err.message);
      res.status(500).json({
        success: false,
        message: "Failed to save student and guardian info."
      });
    }

  } catch (err) {
    console.error("❌ DB connection error:", err.message);
    res.status(500).json({
      success: false,
      message: "Database connection failed."
    });
  }
  
});
// ===========================================================
// 👩‍🎓 Maintenance: Fetch All Students (Email + Smartcard only)
// ===========================================================
app.get("/api/maintenance/students", async (req, res) => {
  try {
    const pool = await sql.connect(dbConfig);
    const result = await pool.request().query(`
      SELECT 
        s.S_name        AS name,
        s.S_email_id   AS email,
        s.College_name AS collegeName
      FROM [User] u
      JOIN Student s
        ON u.firebase_uid = s.firebase_uid
      WHERE u.user_type = 'student'
      ORDER BY s.S_name
    `);
    res.status(200).json({
      success: true,
      students: result.recordset
    });

  } catch (err) {
    console.error("❌ Error fetching students:", err.message);
    res.status(500).json({
      success: false,
      message: "Failed to fetch students"
    });
  }
});


// ===========================================================
// 🚗 Maintenance: Fetch All Drivers
// ===========================================================
app.get("/api/maintenance/drivers", async (req, res) => {
  try {
    const pool = await sql.connect(dbConfig);

    const result = await pool.request().query(`
      SELECT
        d.D_name        AS name,
        d.D_eid   AS email,
        d.D_licence_no AS licenceNumber
      FROM [User] u
      JOIN Driver d
        ON u.firebase_uid = d.firebase_uid
      WHERE u.user_type = 'driver'
    `);

    res.status(200).json({
      success: true,
      drivers: result.recordset
    });

  } catch (err) {
    console.error("❌ Error fetching drivers:", err.message);
    res.status(500).json({
      success: false,
      message: "Failed to fetch drivers"
    });
  }
});
  // ===========================================================
// 🛡️ Admin: Fetch PENDING Driver Verifications
// ===========================================================
app.get("/api/admin/drivers/pending", async (req, res) => {
  try {
    const pool = await sql.connect(dbConfig);

    const result = await pool.request().query(`
      SELECT
        d.D_eid,
        d.D_name,
        d.D_phone_no,
        d.D_licence_no,
        v.college_name,
        v.admin_email,
        v.verification_status
      FROM Driver_Verification v
      JOIN Driver d
        ON v.D_eid = d.D_eid
      WHERE v.verification_status = 'PENDING'
      ORDER BY d.D_name
    `);

    res.status(200).json({
      success: true,
      drivers: result.recordset
    });

  } catch (err) {
    console.error("❌ Error fetching pending drivers:", err.message);
    res.status(500).json({
      success: false,
      message: "Failed to fetch pending drivers"
    });
  }
});

// ===========================================================
// ✅ Admin: Verify / Reject Driver
// ===========================================================
/*app.put("/api/admin/drivers/verify", async (req, res) => {
  const {
    D_eid,
    college_name,
    verification_status // APPROVED or REJECTED
  } = req.body;

  // 🔴 Validation
  if (!D_eid || !college_name || !verification_status) {
    return res.status(400).json({
      success: false,
      message: "Missing required fields"
    });
  }

  if (!["APPROVED", "REJECTED"].includes(verification_status)) {
    return res.status(400).json({
      success: false,
      message: "Invalid verification status"
    });
  }

  try {
    const pool = await sql.connect(dbConfig);

    const result = await pool.request()
      .input("D_eid", sql.VarChar, D_eid)
      .input("college_name", sql.VarChar, college_name)
      .input("verification_status", sql.VarChar, verification_status)
      .query(`
        UPDATE Driver_Verification
        SET
          verification_status = @verification_status,
          verified_at = GETDATE()
        WHERE
          D_eid = @D_eid
          AND college_name = @college_name
      `);

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({
        success: false,
        message: "Verification record not found"
      });
    }

    res.json({
      success: true,
      message: `Driver ${verification_status.toLowerCase()} successfully`
    });

  } catch (err) {
    console.error("❌ Error verifying driver:", err.message);
    res.status(500).json({
      success: false,
      message: "Failed to update verification status"
    });
  }
});*/
app.put("/api/admin/drivers/verify", async (req, res) => {
  const { D_eid, college_name, admin_email, verification_status } = req.body;

  if (!D_eid || !college_name || !admin_email || !verification_status) {
    return res.status(400).json({
      success: false,
      message: "Missing required fields"
    });
  }

  if (!["APPROVED", "REJECTED"].includes(verification_status)) {
    return res.status(400).json({
      success: false,
      message: "Invalid verification status"
    });
  }

  try {
    const pool = await sql.connect(dbConfig);

    // 1️⃣ Check if verification already exists
    const check = await pool.request()
      .input("D_eid", sql.VarChar, D_eid)
      .input("college_name", sql.VarChar, college_name)
      .query(`
        SELECT * FROM Driver_Verification
        WHERE D_eid = @D_eid AND college_name = @college_name
      `);

    if (check.recordset.length === 0) {
      // 2️⃣ INSERT (first time verification)
      await pool.request()
        .input("D_eid", sql.VarChar, D_eid)
        .input("college_name", sql.VarChar, college_name)
        .input("admin_email", sql.VarChar, admin_email)
        .input("verification_status", sql.VarChar, verification_status)
        .query(`
          INSERT INTO Driver_Verification (
            D_eid, college_name, admin_email, verification_status, verified_at
          )
          VALUES (
            @D_eid, @college_name, @admin_email, @verification_status, GETDATE()
          )
        `);
    } else {
      // 3️⃣ UPDATE (already exists)
      await pool.request()
        .input("D_eid", sql.VarChar, D_eid)
        .input("college_name", sql.VarChar, college_name)
        .input("verification_status", sql.VarChar, verification_status)
        .query(`
          UPDATE Driver_Verification
          SET verification_status = @verification_status,
              verified_at = GETDATE()
          WHERE D_eid = @D_eid AND college_name = @college_name
        `);
    }

    res.json({
      success: true,
      message: `Driver ${verification_status.toLowerCase()} successfully`
    });

  } catch (err) {
    console.error("❌ Error verifying driver:", err.message);
    res.status(500).json({
      success: false,
      message: "Verification failed"
    });
  }
});


// ===========================================================
// 🎓 GET STUDENT DETAILS BY FIREBASE UID
// ===========================================================

app.get("/getStudentDetails/:firebase_uid", async (req, res) => {
  const { firebase_uid } = req.params;

  try {
    // 🔥 FIX: pool yahin create karo
    const pool = await sql.connect(dbConfig);

    const result = await pool
      .request()
      .input("firebase_uid", sql.VarChar, firebase_uid)
      .query(`
        SELECT 
          s.firebase_uid,
          s.S_email_id,
          s.S_name,
          s.Smartcard_id,
          s.College_name,
          s.dateofbirth,
          s.gender,
          s.aadhar_number,
          s.course,
          s.branch,
          s.year,
          s.Permanent_address,
          s.hostel,
          p.G_name      AS guardian_name,
          p.G_phone_no AS guardian_phone,
          p.G_eid      AS guardian_email
        FROM Student s
        LEFT JOIN Parents_Information p
          ON s.S_email_id = p.S_email_id
        WHERE s.firebase_uid = @firebase_uid
      `);

    if (result.recordset.length === 0) {
      return res.status(404).json({
        success: false,
        message: "Student not found"
      });
    }

    res.json({
      success: true,
      data: result.recordset[0]
    });

  } catch (err) {
    console.error("Error fetching student details:", err.message);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});


// ===========================================================
// ✅ Get Student + Guardian Details by Firebase UID
// ==============================================================
/*app.get("/getStudentDetails/:firebase_uid", async (req, res) => {
  const { firebase_uid } = req.params;

  if (!firebase_uid) {
    return res.status(400).json({ success: false, message: "firebase_uid is required" });
  }

  try {
    const pool = await sql.connect(dbConfig);
    const result = await pool.request()
      .input("firebase_uid", sql.VarChar, firebase_uid)
      .query(`
        SELECT 
          s.firebase_uid, s.S_email_id, s.S_name, s.Smartcard_id,
          s.College_name, s.dateofbirth, s.gender, s.aadhar_number,
          s.course, s.branch, s.year, s.Permanent_address, s.hostel, s.created_at,
          p.G_name AS guardian_name, p.G_phone_no AS guardian_phone, p.G_eid AS guardian_email
        FROM Student s
        LEFT JOIN Parents_Information p ON s.S_email_id = p.S_email_id
        WHERE s.firebase_uid = @firebase_uid
      `);

    if (result.recordset.length === 0) {
      return res.status(404).json({ success: false, message: "Student not found" });
    }

    // If there are multiple parent rows, this returns the first one.
    const row = result.recordset[0];
    res.json({ success: true, data: row });

  } catch (err) {
    console.error("❌ Error in /getStudentDetails:", err.message);
    res.status(500).json({ success: false, message: "Server error" });
  }
});*/
///////////////////////////////////////////////////////
app.put("/updateStudentProfile", async (req, res) => {
  const {
    firebase_uid,
    course,
    branch,
    year,
    Permanent_address,
    hostel,
    G_name,
    G_phone_no,
    G_eid
  } = req.body;

  if (!firebase_uid) {
    return res.status(400).json({ success: false, message: "firebase_uid required" });
  }

  const pool = await sql.connect(dbConfig);
  const tx = new sql.Transaction(pool);

  try {
    await tx.begin();

    // 1️⃣ UPDATE STUDENT (sirf selected fields)
    await tx.request()
      .input("firebase_uid", sql.VarChar, firebase_uid)
      .input("course", sql.VarChar, course)
      .input("branch", sql.VarChar, branch)
      .input("year", sql.VarChar, year)
      .input("Permanent_address", sql.VarChar, Permanent_address)
      .input("hostel", sql.VarChar, hostel)
      .query(`
        UPDATE Student SET
          course = @course,
          branch = @branch,
          year = @year,
          Permanent_address = @Permanent_address,
          hostel = @hostel
        WHERE firebase_uid = @firebase_uid
      `);

    // 2️⃣ GET student email (guardian ke liye)
    const emailRes = await tx.request()
      .input("firebase_uid", sql.VarChar, firebase_uid)
      .query(`SELECT S_email_id FROM Student WHERE firebase_uid=@firebase_uid`);

    const S_email_id = emailRes.recordset[0]?.S_email_id;
    if (!S_email_id) throw new Error("Student not found");

    // 3️⃣ UPSERT GUARDIAN
    const guardianUpdate = await tx.request()
      .input("S_email_id", sql.VarChar, S_email_id)
      .input("G_name", sql.VarChar, G_name)
      .input("G_phone_no", sql.VarChar, G_phone_no)
      .input("G_eid", sql.VarChar, G_eid)
      .query(`
        UPDATE Parents_Information
        SET
          G_name=@G_name,
          G_phone_no=@G_phone_no,
          G_eid=@G_eid
        WHERE S_email_id=@S_email_id
      `);

    if (guardianUpdate.rowsAffected[0] === 0) {
      await tx.request()
        .input("S_email_id", sql.VarChar, S_email_id)
        .input("G_name", sql.VarChar, G_name)
        .input("G_phone_no", sql.VarChar, G_phone_no)
        .input("G_eid", sql.VarChar, G_eid)
        .query(`
          INSERT INTO Parents_Information
          (S_email_id, G_name, G_phone_no, G_eid)
          VALUES (@S_email_id, @G_name, @G_phone_no, @G_eid)
        `);
    }

    await tx.commit();
    res.json({ success: true, message: "Profile updated successfully" });

  } catch (e) {
    await tx.rollback();
    res.status(500).json({ success: false, message: e.message });
  }
});
// ===========================================================
// 🎓 Student: Fetch ALL drivers with verification details
// ===========================================================
app.get("/api/student/drivers/all/:college_name", async (req, res) => {
  const { college_name } = req.params;

  try {
    const pool = await sql.connect(dbConfig);

    const result = await pool.request()
      .input("college_name", sql.VarChar, college_name)
      .query(`
        SELECT
          d.D_eid,
          d.D_name,
          d.D_phone_no,
          d.D_licence_no,
          d.current_city,
          d.cost_per_km,

          -- Verified by THIS student's college?
          CASE 
            WHEN EXISTS (
              SELECT 1 FROM Driver_Verification v
              WHERE v.D_eid = d.D_eid
                AND v.college_name = @college_name
                AND v.verification_status = 'APPROVED'
            )
            THEN 1 ELSE 0
          END AS verified_by_my_college,

          -- List of ALL colleges that verified this driver
          (
            SELECT STRING_AGG(v.college_name, ', ')
            FROM Driver_Verification v
            WHERE v.D_eid = d.D_eid
              AND v.verification_status = 'APPROVED'
          ) AS verified_by_colleges

        FROM Driver d
      `);

    res.json({
      success: true,
      drivers: result.recordset
    });

  } catch (err) {
    console.error("❌ Error fetching drivers:", err.message);
    res.status(500).json({
      success: false,
      message: "Failed to fetch drivers"
    });
  }
});

// ===========================================================
// 👤 Get User Role by Firebase UID (For Login - Others)
// ===========================================================
app.post("/api/user/getrole", async (req, res) => {
  const { firebase_uid } = req.body;

  if (!firebase_uid) {
    console.warn("⚠️ Missing firebase_uid in /api/user/getrole:", req.body);
    return res.status(400).json({ success: false, error: "firebase_uid is required" });
  }

  try {
    const pool = await sql.connect(dbConfig);
    const result = await pool.request()
      .input("firebase_uid", sql.VarChar, firebase_uid)
      .query("SELECT user_type FROM [User] WHERE firebase_uid = @firebase_uid");

    if (result.recordset.length > 0) {
      const userType = result.recordset[0].user_type;
      console.log(`✅ Found user role for ${firebase_uid}: ${userType}`);
      return res.status(200).json({ success: true, user_type: userType });
    } else {
      console.warn(`⚠️ No user found for UID: ${firebase_uid}`);
      return res.status(404).json({ success: false, error: "User not found" });
    }
  } catch (err) {
    console.error("❌ Error fetching user role:", err.message);
    return res.status(500).json({ success: false, error: "Database query failed" });
  }
});
// ===========================================================
// ⚠️ Global Error Handler for Invalid JSON
// ===========================================================
app.use((err, req, res, next) => {
  if (err instanceof SyntaxError && "body" in err) {
    console.error("❌ Invalid JSON:", err.message);
    return res.status(400).json({ success: false, error: "Invalid JSON body" });
  }
  next();
});

// ===========================================================
// 🚀 Start Server (Fixed Route Logging)
// ===========================================================
const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT}`);

  // Wait a short moment for Express to register all routes
  setTimeout(() => {
    if (app._router && app._router.stack) {
      console.log("🧩 Registered Routes:");
      app._router.stack
        .filter(r => r.route)
        .forEach(r => {
          const method = Object.keys(r.route.methods)[0].toUpperCase();
          console.log(`${method} ${r.route.path}`);
        });
    } else {
      console.warn("⚠️ No routes found (router not initialized yet).");
    }
  }, 200); // 200ms delay ensures routes exist
});


//=========================================================
// Driver and car info is added
//==================================================
//=========================================================
// Driver and car info is added to database
//==================================================
app.post("/api/driver/add-with-car", async (req, res) => {
  const {
    // DRIVER
    firebase_uid, D_eid, D_name, D_aadhar_no,
    D_phone_no, D_address, D_licence_no,
    D_status,D_avg_rating, D_gender, cost_per_km, current_city, D_dob,


    // CAR
    C_id, C_name, C_number, C_colour,
    C_model, C_ac_nac, C_seater, C_carrier
  } = req.body;

  // 🔴 DRIVER VALIDATION
  if (!firebase_uid || !D_eid || !D_name || !D_phone_no || !D_aadhar_no || !D_licence_no) {
    return res.status(400).json({ success: false, message: "Missing driver details" });
  }

  // 🔴 CAR VALIDATION
  if (!C_id || !C_name || !C_number || !C_model || !C_colour) {
    return res.status(400).json({ success: false, message: "Missing car details" });
  }

  try {
    const pool = await sql.connect(dbConfig);
    const transaction = new sql.Transaction(pool);
    await transaction.begin();

    try {
      // 🔹 INSERT DRIVER
      await transaction.request()
        .input("firebase_uid", sql.VarChar, firebase_uid)
        .input("D_eid", sql.VarChar, D_eid)
        .input("D_name", sql.VarChar, D_name)
       
        .input("D_aadhar_no", sql.VarChar, D_aadhar_no)
        .input("D_phone_no", sql.VarChar, D_phone_no)
        .input("D_address", sql.VarChar, D_address)
        .input("D_licence_no", sql.VarChar, D_licence_no)
        .input("D_status", sql.VarChar, D_status)
        .input("D_gender", sql.VarChar, D_gender)
        .input("cost_per_km", sql.Decimal(6,2), cost_per_km)
        .input("current_city", sql.VarChar, current_city || "Unknown")
        .input("D_dob", sql.Date, D_dob)

        .query(`
          INSERT INTO Driver (
            firebase_uid, D_eid, D_name, D_aadhar_no,
            D_phone_no, D_address, D_licence_no,
            D_status, D_gender, cost_per_km, current_city,D_dob

          )
          VALUES (
            @firebase_uid, @D_eid, @D_name, @D_aadhar_no,
            @D_phone_no, @D_address, @D_licence_no,
            @D_status, @D_gender, @cost_per_km, @current_city,@D_dob
          )
        `);
      // 🔐 INSERT DRIVER VERIFICATION (DEFAULT = PENDING)
/*await transaction.request()
  .input("D_eid", sql.VarChar, D_eid)
  .input("college_name", sql.VarChar, "Banasthali Vidyapith")
  .input("admin_email", sql.VarChar, "admin@banasthali.in")
  .query(`
    INSERT INTO Driver_Verification (
      D_eid,
      college_name,
      admin_email,
      verification_status
    )
    VALUES (
      @D_eid,
      @college_name,
      @admin_email,
      'PENDING'
    )
  `);*/
      // 🔹 INSERT CAR
      await transaction.request()
        .input("C_id", sql.VarChar, C_id)
        .input("C_name", sql.VarChar, C_name)
        .input("C_number", sql.VarChar, C_number)
        .input("D_eid", sql.VarChar, D_eid)
        .input("C_colour", sql.VarChar, C_colour)
        .input("C_model", sql.VarChar, C_model)
        .input("C_ac_nac", sql.VarChar, C_ac_nac)
        .input("C_seater", sql.Int, C_seater)
        .input("C_carrier", sql.VarChar, C_carrier)
        .query(`
          INSERT INTO Car (
            C_id, C_name, C_number, D_eid,
            C_colour, C_model, C_ac_nac, C_seater, C_carrier
          )
          VALUES (
            @C_id, @C_name, @C_number, @D_eid,
            @C_colour, @C_model, @C_ac_nac, @C_seater, @C_carrier
          )
        `);

      await transaction.commit();

      return res.json({
        success: true,
        message: "Driver and Car saved successfully"
      });

    } catch (err) {
      await transaction.rollback();
      console.error("❌ Transaction failed:", err);
      return res.status(500).json({ success: false, message: err.message });
    }

  } catch (err) {
    console.error("❌ DB error:", err);
    return res.status(500).json({ success: false, message: "DB connection error" });
  }
});
// ===========================================================
// 🚗 DRIVER ROUTES
// ===========================================================
// ✅ Fetch Driver Profile
app.get("/driver/profile/:firebase_uid", async (req, res) => {
  try {
    const { firebase_uid } = req.params;
    const pool = await sql.connect(dbConfig);
    const result = await pool
      .request()
      .input("firebase_uid", sql.VarChar, firebase_uid)
      .query("SELECT * FROM Driver WHERE firebase_uid = @firebase_uid");
    if (result.recordset.length === 0) {
      return res.status(404).json({ success: false, message: "Driver not found" });
    }
    res.json({ success: true, data: result.recordset[0] });
  } catch (error) {
    console.error("❌ Error fetching driver:", error);
    res.status(500).json({ success: false, message: "Server error" });
  }
});
// ===========================================================
// 🎓 Student: Fetch ONLY VERIFIED Drivers
// ===========================================================
app.get("/api/student/drivers/verified", async (req, res) => {
  try {
    const pool = await sql.connect(dbConfig);
    const result = await pool.request().query(`
      SELECT DISTINCT
        d.D_eid,
        d.D_name,
        d.D_phone_no,
        d.D_licence_no,
        d.current_city,
        d.cost_per_km,
        d.D_avg_rating
      FROM Driver d
      JOIN Driver_Verification v
        ON d.D_eid = v.D_eid
      WHERE v.verification_status = 'APPROVED'
    `);
    res.status(200).json({
      success: true,
      drivers: result.recordset
    });
  } catch (err) {
    console.error("❌ Error fetching verified drivers:", err.message);
    res.status(500).json({
      success: false,
      message: "Failed to fetch verified drivers"
    });
  }
});
// ✅ Update Driver + Car Profile in azure also
app.put("/driver/profile/:uid", async (req, res) => {
  const { uid } = req.params;
  const {
    D_phone_no,
    D_address,
    D_status,
    cost_per_km,
    current_city,

    // car fields
    C_name,
    C_number,
    C_colour,
    C_model,
    C_ac_nac,
    C_seater,
    C_carrier
  } = req.body;

  try {
    // 1️⃣ Get D_eid from Driver
    const driverRes = await sql.query`
      SELECT D_eid FROM Driver WHERE firebase_uid = ${uid}
    `;

    if (driverRes.recordset.length === 0) {
      return res.json({ success: false, message: "Driver not found" });
    }

    const D_eid = driverRes.recordset[0].D_eid;

    // 2️⃣ Update Driver table
    await sql.query`
      UPDATE Driver SET
        D_phone_no = ${D_phone_no},
        D_address = ${D_address},
        D_status = ${D_status},
        cost_per_km = ${cost_per_km},
        current_city = ${current_city}
      WHERE firebase_uid = ${uid}
    `;

    // 3️⃣ Check if Car exists
    const carRes = await sql.query`
      SELECT COUNT(*) AS cnt FROM Car WHERE D_eid = ${D_eid}
    `;

    const carExists = carRes.recordset[0].cnt > 0;

    if (carExists) {
      // 4️⃣ UPDATE Car
      await sql.query`
        UPDATE Car SET
          C_name = ${C_name},
          C_number = ${C_number},
          C_colour = ${C_colour},
          C_model = ${C_model},
          C_ac_nac = ${C_ac_nac},
          C_seater = ${C_seater},
          C_carrier = ${C_carrier}
        WHERE D_eid = ${D_eid}
      `;
    } else {
      // 5️⃣ INSERT Car (🔥 THIS WAS MISSING)
      await sql.query`
        INSERT INTO Car (
          C_name, C_number, C_colour, C_model,
          C_ac_nac, C_seater, C_carrier, D_eid
        )
        VALUES (
          ${C_name}, ${C_number}, ${C_colour}, ${C_model},
          ${C_ac_nac}, ${C_seater}, ${C_carrier}, ${D_eid}
        )
      `;
    }

    res.json({ success: true, message: "Profile updated successfully" });

  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});
app.get("/driver/profile/full/:uid", async (req, res) => {
  const { uid } = req.params;

  try {
    const result = await sql.query`
      SELECT 
        d.firebase_uid,
        d.D_eid,
        d.D_name,
        d.D_phone_no,
        d.D_address,
        d.D_licence_no,
        d.D_status,
        d.D_avg_rating,
        d.D_gender,
        d.cost_per_km,
        d.current_city,
        d.D_dob,
        c.C_name,
        c.C_number,
        c.C_colour,
        c.C_model,
        c.C_ac_nac,
        c.C_seater,
        c.C_carrier
      FROM Driver d
      LEFT JOIN Car c ON d.D_eid = c.D_eid
      WHERE d.firebase_uid = ${uid}
    `;

    if (result.recordset.length === 0) {
      return res.json({ success: false, message: "Driver not found" });
    }

    const row = result.recordset[0];

    res.json({
      success: true,
      data: {
        firebase_uid: row.firebase_uid,
        D_eid: row.D_eid,
        D_name: row.D_name,
        D_phone_no: row.D_phone_no,
        D_address: row.D_address,
        D_licence_no: row.D_licence_no,
        D_status: row.D_status,
        D_avg_rating: row.D_avg_rating,
        D_gender: row.D_gender,
        cost_per_km: row.cost_per_km,
        current_city: row.current_city,

        // 🚗 NESTED CAR OBJECT (THIS FIXES EVERYTHING)
        car: row.C_name ? {
          C_name: row.C_name,
          C_number: row.C_number,
          C_colour: row.C_colour,
          C_model: row.C_model,
          C_ac_nac: row.C_ac_nac,
          C_seater: row.C_seater,
          C_carrier: row.C_carrier
        } : null
      }
    });

  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});
// ===========================================================
// GET MaintenanceTeam Profile
// ===========================================================
app.get("/maintenance/profile/:uid", async (req, res) => {
  const { uid } = req.params;

  try {
    const result = await sql.query`
      SELECT firebase_uid, MT_email, created_at
      FROM MaintenanceTeam
      WHERE firebase_uid = ${uid}
    `;

    if (result.recordset.length === 0) {
      return res.json({
        success: false,
        message: "Maintenance member not found"
      });
    }

    res.json({
      success: true,
      data: result.recordset[0]
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({
      success: false,
      message: "Server error"
    });
  }
});
////

app.post("/api/search-rides-v2", async (req, res) => {
  console.log("📩 Incoming request body:", req.body);

  try {
    const { pickup, drop } = req.body;

    if (!pickup || !drop) {
      return res.status(400).json({
        success: false,
        message: "Pickup and drop required"
      });
    }

    // 1️⃣ Geocode pickup & drop (FULL address)
    const start = await geocode(pickup);
    const end = await geocode(drop);

    if (!start || !end) {
      return res.status(400).json({
        success: false,
        message: "Location not found"
      });
    }

    // 2️⃣ Calculate road distance (place-to-place)
    const distanceKm = await getDistanceKm(start, end);

    // 3️⃣ DATABASE CONNECTION
    const pool = await sql.connect(dbConfig);

    // 4️⃣ Get all UNIQUE available driver cities
    const cityResult = await pool.request().query(`
      SELECT DISTINCT current_city
      FROM Driver
      WHERE D_status = 'Available'
    `);

    const cities = cityResult.recordset.map(
      row => row.current_city.trim()
    );

    // 5️⃣ Match pickup text with driver city (CASE-INSENSITIVE)
    let matchedCity = null;

    for (const city of cities) {
      if (pickup.toLowerCase().includes(city.toLowerCase())) {
        matchedCity = city;
        break;
      }
    }

    // 6️⃣ If no city matches → return empty drivers
    if (!matchedCity) {
      console.log("❌ No matching city found for pickup:", pickup);
      return res.json({
        success: true,
        pickup,
        drop,
        drivers: []
      });
    }

    // 7️⃣ Fetch drivers for matched city
    const driversResult = await pool.request()
      .input("pickupCity", sql.NVarChar(50), matchedCity)
      .query(`
        SELECT D_eid,D_name, cost_per_km, current_city
        FROM Driver
        WHERE D_status = 'Available'
          AND current_city COLLATE Latin1_General_CI_AS
              = @pickupCity COLLATE Latin1_General_CI_AS
      `);

    // 8️⃣ Fare calculation
    const drivers = driversResult.recordset.map(driver => ({
      id: driver.D_eid,
      name: driver.D_name,
      city: driver.current_city,
      distanceKm: Number(distanceKm.toFixed(2)),
      fare: Number((distanceKm * driver.cost_per_km).toFixed(2))
    }));

    console.log("📤 Sending drivers:", drivers);

    // 9️⃣ Response
    res.json({
      success: true,
      pickup,
      drop,
      drivers
    });

  } catch (err) {
    console.error("❌ Search ride error:", err);
    res.status(500).json({
      success: false,
      error: err.message
    });
  }
});
