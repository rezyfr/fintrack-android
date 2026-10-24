// ac: web-manage-accounts — derives the id of a new account from its name. The rule is the same one
// SaveAccountUseCase uses on Android, so an account added on either device gets the same id:
// trim, uppercase, replace each run of characters outside A-Z and 0-9 with "_", then trim "_".
export function deriveAccountId(name) {
  return name.trim().toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '');
}
