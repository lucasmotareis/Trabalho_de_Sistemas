import AuthGuard from "@/components/layout/AuthGuard";
import AuthenticatedShell from "@/components/layout/AuthenticatedShell";

export default function AuthLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <AuthGuard>
      <AuthenticatedShell>{children}</AuthenticatedShell>
    </AuthGuard>
  );
}
