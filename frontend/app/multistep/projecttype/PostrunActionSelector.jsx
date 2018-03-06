import React from 'react';
import PropTypes from 'prop-types';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class PostrunActionSelector extends React.Component {
    static propTypes = {
        actionsList: PropTypes.array.isRequired,
        valueWasSet: PropTypes.func.isRequired,
        selectedEntries: PropTypes.array
    };

    constructor(props){
        super(props);

        this.state = {
            selectedEntries: []
        }
    }

    componentWillMount(){
        this.setState({
            selectedEntries: this.props.selectedEntries ? this.props.selectedEntries : []
        });
    }

    checkboxUpdated(event, selectedId, cb){
        console.log(event.target);
        if(!event.target.checked){
            console.log("target was checked");
            this.setState({selectedEntries: this.state.selectedEntries.filter(value=>value!==selectedId)}, ()=>cb(this.state.selectedEntries));
        } else {
            console.log("target was unchecked");
            const newval = this.state.selectedEntries;
            newval.push(selectedId);
            this.setState({selectedEntries: newval}, ()=>cb(this.state.selectedEntries));
        }
    }

    render() {
        return <ul className="selection-list">
            {this.props.actionsList.map(action=>
                <li className="selection-list" key={"action-" + action.id}>
                    <label className="selection-list">
                    <input className="selection-list" id={"action-check-" + action.id} type="checkbox"
                           onChange={(event)=>this.checkboxUpdated(event, action.id, this.props.valueWasSet)}
                           defaultChecked={this.state.selectedEntries.includes(action.id)}/>
                        {action.title}
                    </label>
                </li>)}
        </ul>
    }
}

export default PostrunActionSelector;