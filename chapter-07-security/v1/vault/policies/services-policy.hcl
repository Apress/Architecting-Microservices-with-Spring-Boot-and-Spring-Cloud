# Services policy - can read infrastructure secrets they need
path "secret/data/infrastructure/eureka/*" {
  capabilities = ["read"]
}

path "secret/data/infrastructure/config-server/*" {
  capabilities = ["read"]
}

path "secret/metadata/infrastructure/*" {
  capabilities = ["read", "list"]
}
