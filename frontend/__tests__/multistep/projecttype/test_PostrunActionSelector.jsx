import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import PostrunActionSelector from '../../../app/multistep/projecttype/PostrunActionSelector.jsx';
import sinon from 'sinon';
import assert from 'assert';

describe("PostrunActionSelector", ()=>{
    it("should show tickbox selections from props", ()=>{
        const callbackMock = sinon.spy();
        const actionsList = [
            {id: 1, title: "First action"},
            {id: 2, title: "Second action"},
            {id: 3, title: "Third action"},
            {id: 4, title: "Fourth action"}
        ];

        const selectionList = [4,1];

        const rendered = mount(<PostrunActionSelector actionsList={actionsList} valueWasSet={callbackMock} selectedEntries={selectionList}/>);

        expect(rendered.find('li').length).toEqual(4);
        expect(rendered.find('input').at(0).props().id).toEqual("action-check-1");
        expect(rendered.find('input').at(0).props().checked).toBeTruthy();
        expect(rendered.find('label[htmlFor="action-check-1"]').text()).toEqual("First action");

        expect(rendered.find('input').at(3).props().id).toEqual("action-check-4");
        expect(rendered.find('input').at(3).props().checked).toBeTruthy();
        expect(rendered.find('label[htmlFor="action-check-4"]').text()).toEqual("Fourth action");

        expect(rendered.find('input').at(1).props().checked).toBeFalsy();
        expect(rendered.find('input').at(2).props().checked).toBeFalsy();
    });

    it("should notify callback when selection changes", ()=>{
        const callbackMock = sinon.spy();
        const actionsList = [
            {id: 1, title: "First action"},
            {id: 2, title: "Second action"},
            {id: 3, title: "Third action"},
            {id: 4, title: "Fourth action"}
        ];

        const selectionList = [4,1];

        const rendered = mount(<PostrunActionSelector actionsList={actionsList} valueWasSet={callbackMock}
                                                      selectedEntries={selectionList}/>);

        expect(rendered.find('li').length).toEqual(4);
        expect(rendered.find('input').at(0).props().id).toEqual("action-check-1");
        expect(rendered.find('input').at(0).props().checked).toBeTruthy();
        expect(rendered.find('label[htmlFor="action-check-1"]').text()).toEqual("First action");

        rendered.find('input').at(0).simulate("change",{target: rendered.find('input').at(0).props()});
        expect(callbackMock.calledWith([4])).toBeTruthy();

        rendered.find('input').at(0).simulate("change",{target: rendered.find('input').at(0).props()});
        expect(callbackMock.calledWith([4,1])).toBeTruthy();
    });
});