import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import ProjectTypeCompletionComponent from '../../../app/multistep/projecttype/CompletionComponent.jsx';
import sinon from 'sinon';
import assert from 'assert';

const fakeProjectType = {
    name: 'My Kinda Project',
    opensWith: 'MyEditor.app',
    version: "12.0"
};

describe("ProjectTypeCompletionComponent", ()=>{
    beforeEach(() => moxios.install());
    afterEach(() => moxios.uninstall());

    it("should display a summary of the options selected", ()=>{
        const rendered=shallow(<ProjectTypeCompletionComponent projectType={fakeProjectType}/>);

        const summary = rendered.find('SummaryComponent');
        expect(summary.props().name).toEqual(fakeProjectType.name);
        expect(summary.props().opensWith).toEqual(fakeProjectType.opensWith);
        expect(summary.props().version).toEqual(fakeProjectType.version);
    });

    it("should compile an object of data to send to the server", ()=>{
        const rendered=shallow(<ProjectTypeCompletionComponent projectType={fakeProjectType}/>);
        const rtn = rendered.instance().requestContent();
        expect(rtn).toEqual({"name": "My Kinda Project", "opensWith": "MyEditor.app", "targetVersion": "12.0"});
    });

    it("should make a REST call to save data when Confirm is clicked then redirect to index", (done)=>{
        const rendered=shallow(<ProjectTypeCompletionComponent projectType={fakeProjectType}/>);

        const button = rendered.find('button');
        button.simulate('click');
        window.location.assign = sinon.spy();

        return moxios.wait(()=>{
            expect(moxios.requests.mostRecent().config.url).toEqual('/api/projecttype');
            const request = moxios.requests.mostRecent();
            request.respondWith({
                status: 200,
                response: {'status': 'ok'}
            }).then(() => {
                assert(window.location.assign.calledWith('/projecttype/'));
                done();
            }).catch(error=>{
                console.error(error);
                done(error);
            })
        });
    })
});