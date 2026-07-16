USE campus_product;

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT 'Java Web 开发教材', '软件工程专业常用教材，书页干净，配套案例适合课程设计和期末复习。', 30.00, '教材', '/assets/products/real/java-book.jpg', '图书馆一楼大厅', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = 'Java Web 开发教材');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '高等数学复习资料', '高数重点题型整理，含公式速记和典型例题，适合考前集中复习。', 18.00, '教材', '/assets/products/real/math-book.jpg', '教学楼 A 区门口', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '高等数学复习资料');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '大学英语四级词汇书', '词汇书保存完好，重点单词有少量标注，适合备考四六级。', 16.00, '教材', '/assets/products/real/english-book.jpg', '宿舍区快递站旁', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '大学英语四级词汇书');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '期末考试复习笔记', '多门公共课复习提纲和重点笔记，内容清晰，适合临考查漏补缺。', 12.00, '教材', '/assets/products/real/exam-notes.jpg', '二食堂门口', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '期末考试复习笔记');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '蓝牙耳机', '日常通勤和自习室可用，连接稳定，附充电盒，成色较新。', 59.00, '数码', '/assets/products/real/earbuds.jpg', '信息楼大厅', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '蓝牙耳机');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '机械键盘', '适合写代码和办公使用，按键反馈清晰，外观干净。', 88.00, '数码', '/assets/products/real/keyboard.jpg', '实验楼 3 楼', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '机械键盘');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '显示器增高架', '宿舍桌面收纳神器，可放键盘和书本，让学习桌更整洁。', 22.00, '生活', '/assets/products/real/monitor-stand.jpg', '男生宿舍 5 栋楼下', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '显示器增高架');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '护眼台灯', '亮度可调，适合宿舍晚间学习使用，灯光柔和不刺眼。', 35.00, '生活', '/assets/products/real/desk-lamp.jpg', '女生宿舍 2 栋楼下', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '护眼台灯');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '校园通勤自行车', '适合校内通勤，车况良好，刹车正常，毕业转让。', 160.00, '生活', '/assets/products/real/bike.jpg', '北门停车棚', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '校园通勤自行车');

INSERT INTO products (title, description, price, category, image_url, trade_place, seller_id, status, created_at, updated_at)
SELECT '篮球', '室外场使用过几次，手感不错，适合日常训练和寝室组队。', 28.00, '运动', '/assets/products/real/basketball.jpg', '体育馆门口', 1, 'ON_SALE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE title = '篮球');
