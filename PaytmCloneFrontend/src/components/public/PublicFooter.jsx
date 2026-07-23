import React from "react";
import { Wallet, Twitter, Instagram, Linkedin } from "lucide-react";
import { LABELS } from "@/config/labels.config";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export const PublicFooter = () => {
  const labels = LABELS.publicComponents.publicFooter;

  return (
    <footer className="w-full bg-background border-t border-border mt-auto">
      <div className="container mx-auto max-w-7xl px-6 py-12 md:py-16">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-10">
          <div className="md:col-span-4 space-y-4">
            <div className="flex items-center gap-2">
              <div className="bg-primary p-1.5 rounded-md">
                <Wallet className="w-5 h-5 text-primary-foreground" />
              </div>
              <h1 className="font-black text-xl leading-none tracking-tighter text-foreground italic">{labels.brand}</h1>
            </div>
            <p className="text-muted-foreground text-sm max-w-xs leading-relaxed">{labels.description}</p>
            <div className="flex gap-4 pt-2">
              <a href="#" className="text-muted-foreground hover:text-primary transition-colors"><Twitter className="w-5 h-5" /></a>
              <a href="#" className="text-muted-foreground hover:text-primary transition-colors"><Instagram className="w-5 h-5" /></a>
              <a href="#" className="text-muted-foreground hover:text-primary transition-colors"><Linkedin className="w-5 h-5" /></a>
            </div>
          </div>

          <div className="md:col-span-2 space-y-4">
            <h4 className="text-sm font-semibold uppercase tracking-wider text-foreground">{labels.company}</h4>
            <nav className="flex flex-col gap-2 text-sm text-muted-foreground">
              <a href="#" className="hover:text-primary transition-colors">{labels.about}</a>
              <a href="#" className="hover:text-primary transition-colors">{labels.careers}</a>
              <a href="#" className="hover:text-primary transition-colors">{labels.contact}</a>
            </nav>
          </div>

          <div className="md:col-span-2 space-y-4">
            <h4 className="text-sm font-semibold uppercase tracking-wider text-foreground">{labels.legal}</h4>
            <nav className="flex flex-col gap-2 text-sm text-muted-foreground">
              <a href="#" className="hover:text-primary transition-colors">{labels.privacy}</a>
              <a href="#" className="hover:text-primary transition-colors">{labels.terms}</a>
              <a href="#" className="hover:text-primary transition-colors">{labels.cookies}</a>
            </nav>
          </div>

          <div className="md:col-span-4 space-y-4">
            <h4 className="text-sm font-semibold uppercase tracking-wider text-foreground">{labels.newsletterTitle}</h4>
            <p className="text-sm text-muted-foreground">{labels.newsletterSubtitle}</p>
            <div className="flex w-full max-w-sm items-center space-x-2">
              <Input type="email" placeholder={labels.emailPlaceholder} />
              <Button>{labels.subscribe}</Button>
            </div>
          </div>
        </div>

        <div className="mt-12 pt-8 border-t border-border flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-xs text-muted-foreground">{labels.copyright}</p>
        </div>
      </div>
    </footer>
  );
};
