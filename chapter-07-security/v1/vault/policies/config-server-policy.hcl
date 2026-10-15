# Config Server policy - can read all infrastructure secrets
path "secret/data/infrastructure/*" {
  capabilities = ["read"]
}

path "secret/metadata/infrastructure/*" {
  capabilities = ["read", "list"]
}
