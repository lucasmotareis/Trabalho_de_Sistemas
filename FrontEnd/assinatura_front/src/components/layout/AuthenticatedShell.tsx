"use client";

import { useMemo, useState } from "react";
import NextLink from "next/link";
import { usePathname, useRouter } from "next/navigation";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Drawer from "@mui/material/Drawer";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { clearStoredSession, getStoredSession } from "@/lib/auth";
import { authService } from "@/services/auth.service";

const drawerWidth = 240;

const navItems = [
  { href: "/sign", label: "Assinar texto" },
  { href: "/dashboard/verify", label: "Verificar assinatura" },
];

export default function AuthenticatedShell({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  const userName = useMemo(() => getStoredSession()?.user.name ?? "Usuario", []);

  const handleLogout = async () => {
    try {
      await authService.logout();
    } finally {
      clearStoredSession();
      router.push("/login");
    }
  };

  const drawerContent = (
    <Box sx={{ p: 2 }}>
      <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
        Assinatura Digital
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {userName}
      </Typography>
      <List>
        {navItems.map((item) => (
          <ListItemButton
            key={item.href}
            component={NextLink}
            href={item.href}
            selected={pathname === item.href}
            onClick={() => setMobileOpen(false)}
            sx={{ borderRadius: 2, mb: 0.5 }}
          >
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: "flex", minHeight: "100dvh" }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
          borderBottom: 1,
          borderColor: "divider",
        }}
      >
        <Toolbar sx={{ gap: 1 }}>
          <Button
            variant="outlined"
            size="small"
            onClick={() => setMobileOpen((prev) => !prev)}
            sx={{ display: { md: "none" } }}
          >
            Menu
          </Button>
          <Typography variant="subtitle1" sx={{ fontWeight: 600, flexGrow: 1 }}>
            Area autenticada
          </Typography>
          <Button onClick={handleLogout}>Sair</Button>
        </Toolbar>
      </AppBar>

      <Box
        component="nav"
        sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
        aria-label="navigation"
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: "block", md: "none" },
            "& .MuiDrawer-paper": { boxSizing: "border-box", width: drawerWidth },
          }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: "none", md: "block" },
            "& .MuiDrawer-paper": { boxSizing: "border-box", width: drawerWidth },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{ flexGrow: 1, width: { md: `calc(100% - ${drawerWidth}px)` }, p: 3 }}
      >
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
