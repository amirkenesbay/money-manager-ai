print('Start #################################################################');

db = db.getSiblingDB('moneyManagerBot');

db.createUser({
    user: 'moneyManagerUser',
    pwd: 'vfaZPswtZePqyABJ',
    roles: [
        {
            role: 'readWrite',
            db: 'moneyManager',
        },
    ],
});
