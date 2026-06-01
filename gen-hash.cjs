const { execSync } = require('child_process');
execSync('npm install bcryptjs', { cwd: 'C:/Users/12856/Desktop/论文实现/library-system-v2/frontend', stdio: 'ignore' });
const bcrypt = require('C:/Users/12856/Desktop/论文实现/library-system-v2/frontend/node_modules/bcryptjs');
const fs = require('fs');
const hash = bcrypt.hashSync('admin123', 10);
fs.writeFileSync('C:/Users/12856/Desktop/论文实现/library-system-v2/hash.txt', hash);
console.log('HASH WRITTEN');
