<img width="1919" height="877" alt="image" src="https://github.com/user-attachments/assets/8c1beee0-abb7-4dc1-bd6f-ecd9bd3a29ee" />
<img width="1919" height="945" alt="image" src="https://github.com/user-attachments/assets/a26f9719-d150-4ab5-90c0-2e36319d4c23" />
<img width="1919" height="945" alt="image" src="https://github.com/user-attachments/assets/36791250-e11c-4248-ad32-bef914c40ba2" />
<img width="1919" height="884" alt="image" src="https://github.com/user-attachments/assets/05505a3e-b179-43bd-a1e6-4dca7fc23c8c" />
<img width="1919" height="887" alt="image" src="https://github.com/user-attachments/assets/6605de5f-20f0-4ec0-b2b2-2cec39036443" />

Before Migration Environment variables:
<img width="982" height="863" alt="image" src="https://github.com/user-attachments/assets/79fe501a-19d0-42d9-9178-c04ff0a5dc46" />

After migration, 
Setting TF_VAR_enable_migration_host = false

Remove these temporary variables:
TF_VAR_migration_operator_cidr
TF_VAR_migration_host_ssh_public_key
TF_VAR_migration_host_vm_size

as basically we no longer require the VM and in the apply as well after setting that variable to false, it gonna delete the resources but just not make any confusions, we are 
deleting these environment variables.

After migration,
<img width="1113" height="838" alt="image" src="https://github.com/user-attachments/assets/753807cd-a1e7-4ff6-b3a6-e429921b5fc1" />


