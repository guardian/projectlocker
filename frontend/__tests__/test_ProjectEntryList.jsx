import React from 'react';
import moxios from 'moxios';
import {shallow, mount} from 'enzyme';
import ProjectEntryList from '../app/ProjectEntryList.jsx';

describe("ProjectEntryList", ()=>{
    beforeEach(()=>moxios.install());
    afterEach(()=>moxios.uninstall());

    it("should load in configuration on mount", (done)=>{
        window.React = React;

        const rendered = shallow(<ProjectEntryList location={{pathname: "/project"}}/>);

        return moxios.wait(()=>{
            try{
                console.log(moxios.requests);
                const req = moxios.requests.mostRecent();
                expect(req).toBeTruthy();
                expect(req.config.url).toEqual("/api/system/plutoconfig");
                req.respondWith({
                    status: 200,
                    response: {
                        plutoServer: "https://localhost:8000",
                        syncEnabled: "yes",
                        siteName: "VX"
                    }
                }).then(()=>{
                    expect(rendered.instance().state.plutoConfig).toEqual({
                        plutoServer: "https://localhost:8000",
                        syncEnabled: "yes",
                        siteName: "VX"
                    });
                    done();
                }).catch(err=>done.fail(err));
            } catch(e) {
                done.fail(e);
            }
        })
    });

});