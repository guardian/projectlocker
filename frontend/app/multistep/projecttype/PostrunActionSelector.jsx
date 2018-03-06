import React from 'react';
import PropTypes from 'prop-types';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class PostrunActionSelector extends React.Component {
    static propTypes = {
        actionsList: PropTypes.array.isRequired,
        loadErrors: PropTypes.object,
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
        if(event.target.checked){
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
        if(this.props.loadErrors) return <ErrorViewComponent error={this.state.loadErrors}/>;

        return <ul className="selection-list">
            {this.props.actionsList.map(action=>
                <li key={"action-" + action.id}>
                    <label htmlFor={"action-check-" + action.id}>{action.title}</label>
                    <input id={"action-check-" + action.id} type="checkbox"
                           onChange={(event)=>this.checkboxUpdated(event, action.id, this.props.valueWasSet)}
                           checked={this.state.selectedEntries.includes(action.id)}
                    />
                </li>)}
        </ul>
    }
}

export default PostrunActionSelector;