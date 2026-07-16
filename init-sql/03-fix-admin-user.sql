USE campus_user;

UPDATE users
SET nickname = '管理员',
    phone = '13800000000',
    campus = '主校区'
WHERE username = 'admin';
