const express = require('express');
const mysql = require('mysql2/promise');

const app = express();
app.use(express.json());

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || 'root',
  database: process.env.DB_NAME || 'apisuite_db',
  connectionLimit: 5,
});

const isValid = (b) => b && typeof b.name === 'string' && b.name.trim() &&
                       typeof b.email === 'string' && b.email.trim();

app.get('/api/users', async (req, res) => {
  const [rows] = await pool.query('SELECT id, name, email, role FROM users ORDER BY id');
  res.json(rows);
});

app.get('/api/users/:id', async (req, res) => {
  const [rows] = await pool.query('SELECT id, name, email, role FROM users WHERE id = ?', [req.params.id]);
  if (rows.length === 0) return res.status(404).json({ message: 'User not found' });
  res.json(rows[0]);
});

app.post('/api/users', async (req, res) => {
  if (!isValid(req.body)) return res.status(400).json({ message: 'name and email are required' });
  const { name, email, role = 'user' } = req.body;
  try {
    const [result] = await pool.query(
      'INSERT INTO users (name, email, role) VALUES (?, ?, ?)', [name, email, role]);
    res.status(201).json({ id: result.insertId, name, email, role });
  } catch (e) {
    if (e.code === 'ER_DUP_ENTRY') return res.status(409).json({ message: 'Email already exists' });
    res.status(500).json({ message: 'Server error' });
  }
});

app.put('/api/users/:id', async (req, res) => {
  if (!isValid(req.body)) return res.status(400).json({ message: 'name and email are required' });
  const { name, email, role = 'user' } = req.body;
  const [result] = await pool.query(
    'UPDATE users SET name = ?, email = ?, role = ? WHERE id = ?', [name, email, role, req.params.id]);
  if (result.affectedRows === 0) return res.status(404).json({ message: 'User not found' });
  res.json({ id: Number(req.params.id), name, email, role });
});

app.delete('/api/users/:id', async (req, res) => {
  const [result] = await pool.query('DELETE FROM users WHERE id = ?', [req.params.id]);
  if (result.affectedRows === 0) return res.status(404).json({ message: 'User not found' });
  res.status(204).send();
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => console.log(`Users API running on http://localhost:${PORT}/api`));
